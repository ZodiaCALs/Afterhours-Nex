package app.afterhours.ui

import kotlin.math.abs
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.afterhours.core.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** The speeds to choose from, in the speed menu and in Settings. */
val SPEED_CHOICES = listOf(0.5f, 0.75f, 0.9f, 1f, 1.1f, 1.2f, 1.25f, 1.3f, 1.5f, 1.75f, 2f, 2.5f, 3f)

/** "1.25×", "2×". */
fun speedLabel(s: Float): String {
    val r = (s * 100).roundToInt() / 100.0
    return (if (r % 1.0 == 0.0) "${r.toInt()}" else r.toString().trimEnd('0').trimEnd('.')) + "×"
}

/** Now Playing, as in the web app: the cover blurred behind everything, the cover and controls, and the episode's notes. */
@Composable
fun NowPlaying(app: AppState) {
    val ep by app.playback.current.collectAsState()
    val st by app.playback.state.collectAsState()
    val settings by app.library.settings.collectAsState()
    val e = ep
    if (e == null) {
        LaunchedEffect(Unit) { app.nowPlaying = false }
        return
    }
    val show = app.library.show(e.sid)
    val cap by app.playback.captions.collectAsState()
    val lyrics = settings.captions && cap.cues.isNotEmpty()
    val art = e.image.ifBlank { show?.art.orEmpty() }
    val design = if (e.image.isBlank()) show?.coverStyle.orEmpty() else ""
    val name = show?.name ?: e.title
    LookProvider(DarkLook) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF101014)).pointerInput(Unit) { detectTapGestures { } }) {
            if (settings.lessEffects) Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.hsl(hueOf(name), 0.45f, 0.28f), Color(0xFF101014)))))
            else Artwork(art, name, Modifier.fillMaxSize().scale(1.35f).blur(70.dp), corner = 0.dp, shadow = false, small = true)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.32f), Color.Black.copy(alpha = 0.58f)))))
            val wide = maxWidth >= 900.dp
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Box(Modifier.fillMaxWidth().padding(top = app.platform.chromeTop)) {
                app.platform.DragArea(Modifier.matchParentSize())
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundButton(WebIcons.Chevdown, tr("Close Now Playing"), { app.nowPlaying = false }, size = 34.dp, bg = Color.White.copy(alpha = 0.16f), tint = Color.White)
                    Text(tr("Now Playing"), style = Type.pill, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    RoundButton(WebIcons.Cc, tr("Captions"), { app.library.updateSettings { it.copy(captions = !it.captions) } }, size = 34.dp,
                        bg = if (settings.captions) Color.White else Color.White.copy(alpha = 0.16f), tint = if (settings.captions) Color(0xFF18181A) else Color.White)
                }
                }
                if (wide) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                        Column(Modifier.width(380.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                            Artwork(art, name, Modifier.fillMaxWidth().aspectRatio(1f), corner = 14.dp, style = design)
                            Spacer(Modifier.height(24.dp))
                            Controls(app, e, show)
                        }
                        if (lyrics) Column(Modifier.weight(1f).fillMaxHeight()) {
                            Lyrics(app, cap.cues, Modifier.weight(1f).fillMaxWidth())
                            CaptionBlock(app)
                            Spacer(Modifier.height(12.dp))
                        } else Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                            Notes(app, e, st.error)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                        Artwork(art, name, Modifier.padding(vertical = 12.dp).widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally), corner = 14.dp, style = design)
                        Controls(app, e, show)
                        if (lyrics) Lyrics(app, cap.cues, Modifier.fillMaxWidth().height(300.dp).padding(top = 8.dp), compact = true)
                        Notes(app, e, st.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun Controls(app: AppState, e: Episode, show: Show?) {
    val st by app.playback.state.collectAsState()
    Text(e.title, style = Type.sectionTitle, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
    Text(show?.name.orEmpty(), style = Type.body, color = Color.White.copy(alpha = 0.7f), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    Scrubber(st.positionMs, st.durationMs, { app.playback.seek(it) }, Color.White.copy(alpha = 0.25f), Color.White, Color.White.copy(alpha = 0.6f),
        Modifier.fillMaxWidth().padding(top = 18.dp))
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconBox(tr("Start of episode"), { app.playback.seek(0) }) { Icon(WebIcons.Prev, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        IconBox(tr("Back 15 seconds"), { app.playback.skip(-15) }) { SkipIcon(true, 15, Color.White, 30.dp) }
        PlayCircle(st.playing, st.buffering, 64.dp, Color.White, Color(0xFF18181A)) { app.playback.toggle() }
        IconBox(tr("Forward 30 seconds"), { app.playback.skip(30) }) { SkipIcon(false, 30, Color.White, 30.dp) }
        IconBox(tr("Next episode"), { if (app.playback.nextEpisode() != null) app.playback.next() }) {
            Icon(WebIcons.Next, contentDescription = null, tint = Color.White.copy(alpha = if (app.playback.nextEpisode() != null) 1f else 0.35f), modifier = Modifier.size(28.dp))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SpeedButton(app, st.speed, Color.White)
        SleepButton(app, Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun Notes(app: AppState, e: Episode, error: String?) {
    CaptionBlock(app)
    error?.let { Text(tr(it), color = Color(0xFFFF8A8A), style = Type.cardSub, modifier = Modifier.padding(vertical = 8.dp)) }
    if (e.desc.isNotBlank()) {
        Text(tr("About this episode"), style = Type.meta, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Text(e.desc, style = Type.pageSub, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 32.dp))
    }
}

/** A round play or pause button (black in the player bar, white in Now Playing). */
@Composable
fun PlayCircle(playing: Boolean, buffering: Boolean, size: Dp, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (buffering && !playing) CircularProgressIndicator(Modifier.size(size * 0.42f), color = fg, strokeWidth = 2.dp)
        else Icon(if (playing) WebIcons.Pause else WebIcons.Play, contentDescription = tr(if (playing) "Pause" else "Play"), tint = fg, modifier = Modifier.size(size * 0.56f))
    }
}

/** A round, borderless button around any icon. */
@Composable
fun IconBox(description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) { content() }
}

/** A thin scrubber: tap or drag to move through the episode. With [inline], the times sit at its ends. */
@Composable
fun Scrubber(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, track: Color, fill: Color, times: Color, modifier: Modifier, inline: Boolean = false) {
    if (durationMs <= 0 && positionMs > 2000) {          // a live stream: no end to scrub to
        Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF453A)))
            Text(tr("LIVE"), style = Type.meta.copy(fontSize = 11.sp, letterSpacing = 1.sp), color = times, modifier = Modifier.padding(start = 6.dp))
        }
        return
    }
    var drag by remember { mutableStateOf<Float?>(null) }
    val length = durationMs.coerceAtLeast(1L)
    val frac = drag ?: (positionMs.toFloat() / length).coerceIn(0f, 1f)
    val at = (frac * length).toLong()
    val left = fmtClock(at / 1000)
    val right = "-" + fmtClock(((durationMs - at).coerceAtLeast(0)) / 1000)
    val line = @Composable { m: Modifier ->
        Box(m.height(16.dp)
            .pointerInput(durationMs) { detectTapGestures { o -> if (durationMs > 0) onSeek((o.x / size.width * durationMs).toLong()) } }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(onDragEnd = { drag?.let { if (durationMs > 0) onSeek((it * durationMs).toLong()) }; drag = null }, onDragCancel = { drag = null }) { change, _ ->
                    drag = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            }, contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().height(if (inline) 3.dp else 4.dp).clip(CircleShape).background(track))
            Box(Modifier.fillMaxWidth(frac).height(if (inline) 3.dp else 4.dp).clip(CircleShape).background(fill))
        }
    }
    if (inline) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(left, style = Type.meta.copy(fontSize = 11.sp), color = times, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            line(Modifier.weight(1f).padding(horizontal = 8.dp))
            Text(right, style = Type.meta.copy(fontSize = 11.sp), color = times, modifier = Modifier.width(44.dp))
        }
    } else {
        Column(modifier) {
            line(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth()) {
                Text(left, style = Type.meta.copy(fontSize = 11.sp), color = times)
                Spacer(Modifier.weight(1f))
                Text(right, style = Type.meta.copy(fontSize = 11.sp), color = times)
            }
        }
    }
}

/** The current speed; tap it for the list of speeds, with 1× marked as normal and a tick by the one in use. */
@Composable
fun SpeedButton(app: AppState, speed: Float, color: Color) {
    var open by remember { mutableStateOf(false) }
    val ia = remember { MutableInteractionSource() }
    Box {
        Text(speedLabel(speed), style = Type.pill, color = color,
            modifier = Modifier.pressable(ia, 0.94f).clip(RoundedCornerShape(8.dp)).hoverFill(ia, RoundedCornerShape(8.dp), color.copy(alpha = 0.12f))
                .clickable(ia, null) { open = true }.semantics { contentDescription = tr("Playback speed") }.padding(horizontal = 10.dp, vertical = 8.dp))
        GlassMenu(open, { open = false }, width = 200.dp) {
            MenuLabel(tr("Playback speed"))
            SPEED_CHOICES.forEach { s ->
                MenuItem(speedLabel(s), { app.playback.speed(s); open = false }, checked = abs(s - speed) < 0.01f, note = if (s == 1f) tr("Normal") else null)
            }
        }
    }
}

/** The moon button and its menu: off, 15 to 60 minutes, or the end of the episode. */
@Composable
fun SleepButton(app: AppState, color: Color) {
    val sleep by app.playback.sleep.collectAsState()
    var open by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Clock.now()) }
    LaunchedEffect(sleep) { while (sleep != null) { now = Clock.now(); delay(15_000) } }
    Box {
        Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { open = true }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(WebIcons.Moon, contentDescription = tr("Sleep timer"), tint = if (sleep != null) look.accent else color, modifier = Modifier.size(18.dp))
            val label = when (val s = sleep) {
                null -> ""
                is Sleep.EndOfEpisode -> tr(" End of episode")
                is Sleep.Minutes -> tr(" ${((s.endsAt - now) / 60_000L).coerceAtLeast(1)} min left")
            }
            if (label.isNotEmpty()) Text(label, style = Type.pill, color = color)
        }
        GlassMenu(open, { open = false }, width = 220.dp) {
            MenuLabel(tr("Sleep timer"))
            MenuItem(tr("Off"), { app.playback.sleepAfter(null); open = false }, checked = sleep == null)
            listOf(15, 30, 45, 60).forEach { m -> MenuItem(tr("$m minutes"), { app.playback.sleepAfter(m); open = false }, checked = (sleep as? Sleep.Minutes)?.minutes == m) }
            MenuItem(tr("End of this episode"), { app.playback.sleepAtEnd(); open = false }, checked = sleep is Sleep.EndOfEpisode)
        }
    }
}
