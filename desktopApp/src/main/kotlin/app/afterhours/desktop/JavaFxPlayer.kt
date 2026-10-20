package app.afterhours.desktop

import app.afterhours.core.AudioPlayer
import app.afterhours.core.PlayItem
import app.afterhours.core.PlayerState
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.Timer
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread

/** Desktop audio through JavaFX's media engine: MP3 and AAC on Windows, macOS and Linux. */
class JavaFxPlayer(private val convertDir: File) : AudioPlayer {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var player: MediaPlayer? = null
    private var id: String? = null
    private var speed = 1.0
    private var ended = false
    private var error: String? = null
    @Volatile private var converting = false
    private val loads = AtomicInteger()
    private val ticker: Timer

    init {
        // Creating a JFXPanel starts JavaFX inside a Swing (and so Compose) app.
        if (SwingUtilities.isEventDispatchThread()) JFXPanel() else SwingUtilities.invokeAndWait { JFXPanel() }
        Platform.setImplicitExit(false)
        ticker = fixedRateTimer("afterhoursnex-position", daemon = true, period = 500L) { Platform.runLater { publish() } }
    }

    override fun load(item: PlayItem, startMs: Long, play: Boolean) {
        val token = loads.incrementAndGet()
        Platform.runLater {
            player?.dispose()
            player = null
            id = item.id
            ended = false
            error = null
            converting = false
            publish()
        }
        thread(isDaemon = true, name = "afterhoursnex-link") {
            val url = if (item.url.startsWith("http")) finalUrl(item.url) else item.url   // JavaFX won't follow a redirect from http to https, so follow them first
            if (Ffmpeg.needsConversion(url)) prepare(url, token, startMs, play)
            else Platform.runLater { open(url, token, startMs, play, converted = false) }
        }
    }

    /** Opus, Ogg, FLAC and the other formats JavaFX can't play are converted with FFmpeg first. */
    private fun prepare(url: String, token: Int, startMs: Long, play: Boolean) {
        converting = true
        Platform.runLater { publish() }
        val file = Ffmpeg.convertForPlayback(url, convertDir) { token != loads.get() }
        converting = false
        Platform.runLater {
            if (token != loads.get()) return@runLater
            if (file == null) { error = "This episode’s audio couldn’t be converted for playback."; publish() }
            else open(file.toURI().toString(), token, startMs, play, converted = true)
        }
    }

    private fun open(url: String, token: Int, startMs: Long, play: Boolean, converted: Boolean) {
        if (token != loads.get()) return
        try {
            val mp = MediaPlayer(Media(url))
            mp.rate = speed
            mp.setOnReady {
                if (startMs > 0) mp.seek(Duration.millis(startMs.toDouble()))
                if (play) mp.play()
                publish()
            }
            mp.setOnEndOfMedia { ended = true; mp.pause(); publish() }
            mp.setOnError {
                if (!converted) {   // JavaFX can't play it after all: convert it instead
                    mp.dispose()
                    if (player === mp) player = null
                    thread(isDaemon = true) { prepare(url, token, startMs, play) }
                } else {
                    error = "This episode’s audio couldn’t be played (${mp.error?.type ?: "unknown problem"})."
                    publish()
                }
            }
            mp.statusProperty().addListener { _, _, _ -> publish() }
            player = mp
        } catch (e: Exception) {
            if (!converted) thread(isDaemon = true) { prepare(url, token, startMs, play) }
            else error = "This episode’s audio couldn’t be played. Its format may not be supported on this computer."
        }
        publish()
    }

    override fun play() = Platform.runLater {
        val mp = player ?: return@runLater
        if (ended) { mp.seek(Duration.ZERO); ended = false }
        mp.play()
    }
    override fun pause() = Platform.runLater { player?.pause() }
    override fun seekTo(ms: Long) = Platform.runLater { ended = false; player?.seek(Duration.millis(ms.coerceAtLeast(0).toDouble())) }
    override fun setSpeed(speed: Float) = Platform.runLater { this.speed = speed.toDouble(); player?.rate = this.speed }

    fun close() {
        ticker.cancel()
        Platform.runLater { player?.dispose() }
    }

    private fun publish() {
        val mp = player
        val total = mp?.totalDuration
        _state.value = PlayerState(
            id = id,
            playing = mp?.status == MediaPlayer.Status.PLAYING,
            buffering = converting || (mp != null && (mp.status == MediaPlayer.Status.UNKNOWN || mp.status == MediaPlayer.Status.STALLED)),
            positionMs = mp?.currentTime?.toMillis()?.toLong() ?: 0,
            durationMs = if (total == null || total.isUnknown || total.isIndefinite) 0 else total.toMillis().toLong(),
            speed = speed.toFloat(),
            ended = ended,
            error = error,
        )
    }

    private fun finalUrl(start: String): String {
        var url = start
        repeat(8) {
            val c = try { URI(url).toURL().openConnection() as HttpURLConnection } catch (e: Exception) { return url }
            try {
                c.instanceFollowRedirects = false
                c.connectTimeout = 10_000
                c.readTimeout = 10_000
                c.setRequestProperty("User-Agent", "AfterhoursNex/${app.afterhours.core.AppInfo.VERSION} (desktop podcast player)")
                c.setRequestProperty("Range", "bytes=0-0")
                val next = c.getHeaderField("Location")
                if (c.responseCode in 300..399 && next != null) url = URI(url).resolve(next).toString() else return url
            } catch (e: Exception) {
                return url
            } finally {
                c.disconnect()
            }
        }
        return url
    }
}
