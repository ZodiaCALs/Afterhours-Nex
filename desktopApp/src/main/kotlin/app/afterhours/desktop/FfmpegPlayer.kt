package app.afterhours.desktop

import app.afterhours.core.AudioPlayer
import app.afterhours.core.PlayItem
import app.afterhours.core.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

/**
 * The light desktop player: the bundled FFmpeg decodes the episode (any format, live radio too) and Java's own sound
 * output plays it. No media framework is loaded, so it takes very little memory. Seeking restarts the decoder at the new
 * place; speed uses FFmpeg's pitch-keeping tempo filter.
 */
class FfmpegPlayer : AudioPlayer {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    private val format = AudioFormat(RATE.toFloat(), 16, 2, true, false)
    private var item: PlayItem? = null
    @Volatile private var generation = 0
    @Volatile private var proc: Process? = null
    @Volatile private var line: SourceDataLine? = null
    @Volatile private var playing = false
    @Volatile private var buffering = false
    @Volatile private var ended = false
    @Volatile private var error: String? = null
    @Volatile private var speed = 1f
    @Volatile private var base = 0L          // where the current run started, in milliseconds of the episode
    @Volatile private var duration = 0L

    init {
        thread(isDaemon = true, name = "audio-clock") { while (true) { Thread.sleep(500); if (playing) publish() } }
    }

    @Synchronized override fun load(item: PlayItem, startMs: Long, play: Boolean) {
        stop()
        this.item = item; duration = 0; ended = false; error = null; base = startMs.coerceAtLeast(0)
        if (play) run(base) else publish()
    }

    @Synchronized override fun play() {
        if (item == null || playing) return
        if (ended) { base = 0; ended = false }
        run(base)
    }

    @Synchronized override fun pause() {
        if (!playing) return
        base = position(); stop(); publish()
    }

    @Synchronized override fun seekTo(ms: Long) {
        val was = playing
        val to = ms.coerceAtLeast(0)
        stop(); base = to; ended = false
        if (was) run(to) else publish()
    }

    @Synchronized override fun setSpeed(speed: Float) {
        if (kotlin.math.abs(this.speed - speed) < 0.001f) return
        val was = playing
        val at = if (was) position() else base
        this.speed = speed
        if (was) { stop(); base = at; run(at) } else publish()
    }

    fun close() = stop()

    private fun position(): Long {
        val frames = line?.longFramePosition ?: return base
        return base + (frames.toDouble() * 1000.0 / RATE * speed).toLong()
    }

    private fun run(fromMs: Long) {
        val url = item?.url ?: return
        val token = ++generation
        base = fromMs; playing = true; buffering = true; publish()
        thread(isDaemon = true, name = "audio") { decode(token, url, fromMs) }
    }

    private fun stop() {
        generation++
        playing = false; buffering = false
        proc?.destroy(); proc = null
        line?.let { l -> runCatching { l.stop(); l.flush(); l.close() } }
        line = null
    }

    private fun decode(token: Int, url: String, fromMs: Long) {
        val args = mutableListOf(Ffmpeg.path, "-nostdin", "-hide_banner", "-loglevel", "info")
        if (url.startsWith("http")) args += listOf("-user_agent", "AfterhoursNex/${app.afterhours.core.AppInfo.VERSION}", "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "4")
        if (fromMs > 0) args += listOf("-ss", String.format(Locale.ROOT, "%.3f", fromMs / 1000.0))
        args += listOf("-i", ffmpegInput(url), "-vn")
        tempo(speed)?.let { args += listOf("-af", it) }
        args += listOf("-f", "s16le", "-acodec", "pcm_s16le", "-ac", "2", "-ar", "$RATE", "pipe:1")
        val problems = StringBuilder()
        try {
            val p = ProcessBuilder(args).start()
            if (token != generation) { p.destroy(); return }
            proc = p
            thread(isDaemon = true, name = "audio-info") {
                p.errorStream.bufferedReader().forEachLine { l ->
                    DURATION.find(l)?.let { m -> duration = (m.groupValues[1].toLong() * 3600 + m.groupValues[2].toLong() * 60) * 1000 + (m.groupValues[3].toDouble() * 1000).toLong() }
                    if (l.contains("rror") || l.contains("Invalid") || l.contains("not found")) problems.appendLine(l)
                }
            }
            val l = AudioSystem.getSourceDataLine(format)
            l.open(format, RATE * 4 / 2)                      // half a second of sound buffered
            if (token != generation) { l.close(); p.destroy(); return }
            line = l
            l.start()
            val buf = ByteArray(RATE * 4 / 10)               // a tenth of a second at a time
            val input = p.inputStream
            while (token == generation) {
                val n = input.readNBytes(buf, 0, buf.size)
                if (n <= 0) break
                if (buffering) { buffering = false; publish() }
                l.write(buf, 0, n - n % 4)
            }
            if (token == generation) {
                l.drain()
                synchronized(this) {
                    if (token != generation) return
                    val failed = l.longFramePosition == 0L
                    base = position(); playing = false; ended = !failed
                    error = if (failed) friendly(problems.toString()) else null
                    line = null; runCatching { l.close() }
                    publish()
                }
            }
        } catch (e: Exception) {
            if (token == generation) synchronized(this) { playing = false; error = "This episode’s audio couldn’t be played."; publish() }
        }
    }

    private fun friendly(log: String) = when {
        log.contains("403") || log.contains("404") -> "This episode’s audio couldn’t be played. The link may have changed, so try refreshing the show."
        else -> "This episode’s audio couldn’t be played. Its format may not be supported on this computer."
    }

    private fun publish() {
        _state.value = PlayerState(id = item?.id, playing = playing, buffering = buffering && playing, positionMs = if (playing) position() else base,
            durationMs = duration, speed = speed, ended = ended, error = error)
    }

    companion object {
        const val RATE = 44100
        private val DURATION = Regex("""Duration: (\d+):(\d\d):(\d\d(?:\.\d+)?)""")

        /** FFmpeg's tempo filter takes 0.5× to 2× at a time, so faster or slower speeds are a chain of them. */
        fun tempo(s: Float): String? {
            if (kotlin.math.abs(s - 1f) < 0.01f) return null
            val parts = mutableListOf<Float>()
            var left = s
            while (left > 2f) { parts += 2f; left /= 2f }
            while (left < 0.5f) { parts += 0.5f; left /= 0.5f }
            parts += left
            return parts.joinToString(",") { "atempo=" + String.format(Locale.ROOT, "%.4f", it) }
        }

        /** True when the bundled FFmpeg opens https addresses itself, as it needs to for most episodes. */
        val usable: Boolean by lazy {
            runCatching {
                val p = ProcessBuilder(Ffmpeg.path, "-hide_banner", "-protocols").redirectErrorStream(true).start()
                val out = p.inputStream.bufferedReader().readText(); p.waitFor()
                AudioSystem.getMixerInfo().isNotEmpty() && out.substringAfter("Input:").substringBefore("Output:").lines().any { it.trim() == "https" }
            }.getOrDefault(false)
        }
    }
}
