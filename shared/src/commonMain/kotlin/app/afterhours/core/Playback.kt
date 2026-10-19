package app.afterhours.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayItem(val id: String, val url: String, val title: String, val show: String, val image: String)

data class PlayerState(
    val id: String? = null,
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val ended: Boolean = false,
    val error: String? = null,
)

/** The platform's audio player: Media3 on Android, and later the desktop player. */
interface AudioPlayer {
    val state: StateFlow<PlayerState>
    fun load(item: PlayItem, startMs: Long, play: Boolean)
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun setSpeed(speed: Float)
}

sealed interface Sleep {
    data class Minutes(val minutes: Int, val endsAt: Long) : Sleep
    data object EndOfEpisode : Sleep
}

/** What's playing: remembers where you are, plays the next episode, and runs the sleep timer. */
class Playback(private val player: AudioPlayer, private val library: Library, private val scope: CoroutineScope) {
    private val _current = MutableStateFlow<Episode?>(null)
    val current: StateFlow<Episode?> = _current.asStateFlow()
    private val _sleep = MutableStateFlow<Sleep?>(null)
    val sleep: StateFlow<Sleep?> = _sleep.asStateFlow()
    val state: StateFlow<PlayerState> get() = player.state

    private var queue: List<Episode> = emptyList()
    private var lastSaved = 0L
    private var lastPlaying = false
    private var finished: String? = null
    private var sleepJob: Job? = null

    init {
        scope.launch { player.state.collect { onState(it) } }
    }

    fun play(ep: Episode, list: List<Episode> = library.sorted(ep.sid)) {
        if (_current.value?.id == ep.id) { toggle(); return }
        _current.value = ep
        queue = list
        finished = null
        val show = library.show(ep.sid)
        val p = library.progress.value[ep.id]
        val startMs = if (p != null && !p.done && p.t > 5) (p.t - 2) * 1000 else 0L
        player.load(PlayItem(ep.id, ep.url, ep.title, show?.title.orEmpty(), ep.image.ifBlank { show?.image.orEmpty() }), startMs, play = true)
        player.setSpeed(library.settings.value.speed)
        loadCaptions(ep)
    }

    fun toggle() { if (player.state.value.playing) player.pause() else player.play() }

    fun skip(seconds: Int) {
        val st = player.state.value
        val target = st.positionMs + seconds * 1000L
        player.seekTo(if (st.durationMs > 0) target.coerceIn(0, st.durationMs) else target.coerceAtLeast(0))
    }

    fun seek(ms: Long) = player.seekTo(ms)

    fun speed(x: Float) {
        player.setSpeed(x)
        library.updateSettings { it.copy(speed = x) }
    }

    fun nextEpisode(): Episode? {
        val cur = _current.value ?: return null
        val i = queue.indexOfFirst { it.id == cur.id }
        return if (i >= 0) queue.getOrNull(i + 1) else null
    }

    fun next(): Boolean {
        val n = nextEpisode() ?: return false
        play(n, queue)
        return true
    }

    fun sleepAfter(minutes: Int?) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes == null) { _sleep.value = null; return }
        _sleep.value = Sleep.Minutes(minutes, Clock.now() + minutes * 60_000L)
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            player.pause()
            _sleep.value = null
        }
    }

    fun sleepAtEnd() {
        sleepJob?.cancel()
        sleepJob = null
        _sleep.value = Sleep.EndOfEpisode
    }

    // ---- captions: saved auto captions first, then the feed's transcript, then auto captions ----
    var engine: CaptionEngine? = null
    private val _captions = MutableStateFlow(CaptionState())
    val captions: StateFlow<CaptionState> = _captions.asStateFlow()
    private var captionJob: Job? = null

    private fun loadCaptions(ep: Episode) {
        captionJob?.cancel()
        _captions.value = CaptionState(status = "Loading captions…", working = true)
        captionJob = scope.launch {
            library.loadCues(ep.id)?.let { _captions.value = CaptionState(it, source = "auto"); return@launch }
            if (ep.transcript.isNotBlank()) {
                try {
                    val f = Net.fetch(ep.transcript, 8 shl 20)
                    val cues = Captions.lines(Captions.parse(f.text, ep.transcript + " " + f.type))
                    if (cues.isNotEmpty()) { _captions.value = CaptionState(cues, source = "feed"); return@launch }
                } catch (e: FetchException) {
                    // no usable transcript: carry on
                }
            }
            _captions.value = CaptionState()
            if (engine != null && library.settings.value.autoCaptions) autoCaption()
        }
    }

    /** Uses a caption file the listener chose. Returns false when it has no timed captions. */
    fun useCaptionFile(name: String, bytes: ByteArray): Boolean {
        val cues = Captions.lines(Captions.parse(bytes.decodeToString(), name))
        if (cues.isEmpty()) return false
        captionJob?.cancel()
        _captions.value = CaptionState(cues, source = "file")
        return true
    }

    /** Recognises the speech in the current episode, starting from where the listener is. */
    fun autoCaption() {
        val ep = _current.value ?: return
        val eng = engine ?: return
        captionJob?.cancel()
        val options = asrOptions(ep)
        val startMs = (player.state.value.positionMs - 2000).coerceAtLeast(0)
        captionJob = scope.launch {
            val found = ArrayList<Cue>()
            _captions.value = CaptionState(status = "Getting ready to recognise speech…", working = true, source = "auto")
            try {
                eng.transcribe(ep.url, startMs, player.state.value.durationMs, options, { message, p -> _captions.update { it.copy(status = message, progress = p) } }) { batch ->
                    found.addAll(batch)
                    found.sortBy { it.start }
                    _captions.update { it.copy(cues = found.toList()) }
                    library.saveCues(ep.id, found)
                }
                _captions.update { it.copy(working = false, status = "", progress = -1f) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _captions.update { it.copy(working = false, status = "", error = e.message ?: "Captions couldn’t be made.") }
            }
        }
    }

    fun stopCaptions() {
        captionJob?.cancel()
        _captions.update { it.copy(working = false, status = "Auto captions stopped", progress = -1f) }
    }

    private fun asrOptions(ep: Episode): AsrOptions {
        val s = library.settings.value
        if (s.pro) return s.asr
        val cjk = (library.show(ep.sid)?.title.orEmpty() + ep.title).any { it in '\u3040'..'\u30ff' || it in '\u3400'..'\u9fff' }
        return AsrOptions(model = if (s.captionModel != "auto") s.captionModel else if (cjk) "small" else "base")
    }

    private fun onState(st: PlayerState) {
        if (_current.value == null && st.id != null) {   // the app reopened while Android kept playing
            library.episode(st.id)?.let { _current.value = it; queue = library.sorted(it.sid); loadCaptions(it) }
        }
        val cur = _current.value ?: return
        if (st.id != cur.id) return
        val now = Clock.now()
        if (st.durationMs > 0 && !st.ended && (now - lastSaved > 5000 || st.playing != lastPlaying)) {
            library.setProgress(cur.id, st.positionMs / 1000, st.durationMs / 1000)
            lastSaved = now
        }
        lastPlaying = st.playing
        if (st.ended && finished != cur.id) {
            finished = cur.id
            library.setProgress(cur.id, st.durationMs / 1000, st.durationMs / 1000, done = true)
            if (_sleep.value == Sleep.EndOfEpisode) { _sleep.value = null; return }
            if (library.settings.value.autoNext) next()
        }
    }
}
