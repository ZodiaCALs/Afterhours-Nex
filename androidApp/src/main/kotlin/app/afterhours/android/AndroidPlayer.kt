package app.afterhours.android

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.afterhours.core.AudioPlayer
import app.afterhours.core.PlayItem
import app.afterhours.core.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

/** Talks to [PlaybackService] through a Media3 controller, so playback carries on when the app is closed. */
class AndroidPlayer(context: Context) : AudioPlayer {
    private val main = Handler(Looper.getMainLooper())
    private val onMain = Executor { main.post(it) }
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private val waiting = ArrayList<(MediaController) -> Unit>()
    private var error: String? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
        override fun onPlayerError(e: PlaybackException) {
            error = "This episode’s audio couldn’t be played. The link may have changed, so try refreshing the show."
            publish()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            error = null
            publish()
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = try { future.get() } catch (e: Exception) { null } ?: return@addListener
            controller = c
            c.addListener(listener)
            waiting.forEach { it(c) }
            waiting.clear()
            publish()
            main.post(object : Runnable {   // the position, twice a second
                override fun run() { publish(); main.postDelayed(this, 500) }
            })
        }, onMain)
    }

    private fun withController(block: (MediaController) -> Unit) {
        main.post { controller?.let(block) ?: waiting.add(block) }
    }

    override fun load(item: PlayItem, startMs: Long, play: Boolean) = withController { c ->
        error = null
        val meta = MediaMetadata.Builder().setTitle(item.title).setArtist(item.show)
            .setArtworkUri(item.image.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }).build()
        c.setMediaItem(MediaItem.Builder().setMediaId(item.id).setUri(item.url).setMediaMetadata(meta).build(), startMs)
        c.prepare()
        c.playWhenReady = play
    }

    override fun play() = withController { it.play() }
    override fun pause() = withController { it.pause() }
    override fun seekTo(ms: Long) = withController { it.seekTo(ms.coerceAtLeast(0)) }
    override fun setSpeed(speed: Float) = withController { it.setPlaybackSpeed(speed) }

    private fun publish() {
        val c = controller ?: return
        val duration = c.duration
        _state.value = PlayerState(
            id = c.currentMediaItem?.mediaId,
            playing = c.isPlaying,
            buffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = if (duration == C.TIME_UNSET || duration < 0) 0 else duration,
            speed = c.playbackParameters.speed,
            ended = c.playbackState == Player.STATE_ENDED,
            error = error,
        )
    }
}
