package app.afterhours.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.afterhours.core.*
import coil3.compose.AsyncImage

/** Cover art, with a coloured monogram while it loads or when there's none. */
@Composable
fun Artwork(url: String, title: String, modifier: Modifier = Modifier, corner: Dp = 10.dp) {
    val hue = remember(title) { ((title.hashCode() and 0x7fffffff) % 360).toFloat() }
    Box(modifier.clip(RoundedCornerShape(corner)).background(Color.hsl(hue, 0.35f, 0.42f)), contentAlignment = Alignment.Center) {
        Text(title.trim().take(1).uppercase(), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun PageTitle(title: String, sub: String?) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(title: String, sub: String?) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A row of choices, one of them selected. */
@Composable
fun Choice(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(value) }.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@Composable
fun EpisodeRow(app: AppState, ep: Episode, showTitle: String? = null, list: List<Episode>? = null) {
    val progress by app.library.progress.collectAsState()
    val current by app.playback.current.collectAsState()
    val st by app.playback.state.collectAsState()
    val p = progress[ep.id]
    val playing = current?.id == ep.id && st.playing
    var expanded by remember(ep.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            val meta = listOfNotNull(showTitle, fmtDate(ep.date).ifBlank { null }).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ep.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (ep.desc.isNotBlank()) {
                Text(ep.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) 40 else 2, overflow = TextOverflow.Ellipsis)
            }
            val status = when {
                p?.done == true -> tr("Played")
                p != null && p.t > 10 && p.d > p.t -> tr(fmtLength(p.d - p.t) + " left")
                else -> fmtLength(ep.duration)
            }
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        FilledTonalIconButton(onClick = { app.playback.play(ep, list ?: app.library.sorted(ep.sid)) }, modifier = Modifier.padding(start = 12.dp)) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = tr(if (playing) "Pause" else "Play"))
        }
    }
}

@Composable
fun ShowTile(show: Show, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(6.dp)) {
        Artwork(show.image, show.title, Modifier.fillMaxWidth().aspectRatio(1f), corner = 12.dp)
        Text(show.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        val sub = when (show.status) { "pending" -> tr("Finding feed…"); "missing" -> tr("Not found automatically"); else -> show.author }
        if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).padding(16.dp), content = content)
}
