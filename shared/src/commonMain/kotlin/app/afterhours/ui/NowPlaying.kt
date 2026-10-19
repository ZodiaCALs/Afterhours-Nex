package app.afterhours.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.afterhours.core.*
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.8f, 1f, 1.2f, 1.5f, 1.8f, 2f)

@Composable
fun NowPlaying(app: AppState) {
    val ep by app.playback.current.collectAsState()
    val st by app.playback.state.collectAsState()
    val sleep by app.playback.sleep.collectAsState()
    val e = ep
    if (e == null) {
        LaunchedEffect(Unit) { app.nowPlaying = false }
        return
    }
    val show = app.library.show(e.sid)
    var dragging by remember { mutableStateOf<Float?>(null) }
    var sleepMenu by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Clock.now()) }
    LaunchedEffect(sleep) { while (sleep != null) { now = Clock.now(); delay(15_000) } }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { app.nowPlaying = false }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = tr("Close Now Playing")) }
                Text(tr("Now Playing"), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Artwork(e.image.ifBlank { show?.image.orEmpty() }, show?.title ?: e.title,
                Modifier.padding(vertical = 20.dp).widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f), corner = 18.dp)
            Text(e.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(show?.title.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            val length = st.durationMs.coerceAtLeast(1L).toFloat()
            val at = dragging ?: st.positionMs.toFloat().coerceAtMost(length)
            Slider(value = at, onValueChange = { dragging = it }, onValueChangeFinished = { dragging?.let { app.playback.seek(it.toLong()) }; dragging = null },
                valueRange = 0f..length, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(fmtClock(at.toLong() / 1000), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text(tr("-" + fmtClock(((st.durationMs - at.toLong()).coerceAtLeast(0)) / 1000)), style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(onClick = { app.playback.skip(-15) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.FastRewind, contentDescription = tr("Back 15 seconds")); Text(tr("15"), style = MaterialTheme.typography.labelSmall) }
                }
                FilledIconButton(onClick = { app.playback.toggle() }, modifier = Modifier.size(72.dp)) {
                    if (st.buffering && !st.playing) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    else Icon(if (st.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = tr(if (st.playing) "Pause" else "Play"), modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { app.playback.skip(30) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.FastForward, contentDescription = tr("Forward 30 seconds")); Text(tr("30"), style = MaterialTheme.typography.labelSmall) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val i = SPEEDS.indexOfFirst { it >= st.speed - 0.01f }
                    app.playback.speed(SPEEDS[(if (i < 0) 0 else i + 1) % SPEEDS.size])
                }) { Icon(Icons.Filled.Speed, contentDescription = tr("Playback speed")); Text(tr(" ${(st.speed * 10).toInt() / 10.0}×")) }
                Box {
                    TextButton(onClick = { sleepMenu = true }) {
                        Icon(Icons.Filled.Bedtime, contentDescription = null)
                        Text(tr(when (val s = sleep) {
                            null -> tr(" Sleep timer")
                            is Sleep.EndOfEpisode -> tr(" End of episode")
                            is Sleep.Minutes -> tr(" ${((s.endsAt - now) / 60_000L).coerceAtLeast(1)} min left")
                        }))
                    }
                    DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                        DropdownMenuItem(text = { Text(tr("Off")) }, onClick = { app.playback.sleepAfter(null); sleepMenu = false })
                        listOf(15, 30, 45, 60).forEach { m -> DropdownMenuItem(text = { Text(tr("$m minutes")) }, onClick = { app.playback.sleepAfter(m); sleepMenu = false }) }
                        DropdownMenuItem(text = { Text(tr("End of this episode")) }, onClick = { app.playback.sleepAtEnd(); sleepMenu = false })
                    }
                }
                IconButton(enabled = app.playback.nextEpisode() != null, onClick = { app.playback.next() }) { Icon(Icons.Filled.SkipNext, contentDescription = tr("Next episode")) }
            }
            CaptionBlock(app, st.positionMs)
            st.error?.let { Text(tr(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp)) }
            if (e.desc.isNotBlank()) {
                Text(tr("About this episode"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp))
                Text(e.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))
            }
        }
    }
}
