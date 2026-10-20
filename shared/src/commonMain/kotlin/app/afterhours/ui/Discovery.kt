package app.afterhours.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.afterhours.core.*
import kotlinx.coroutines.launch

@Composable
private fun pad() = PaddingValues(bottom = 32.dp + LocalBottomSpace.current)

/** A show seen before adding it: its episodes play from here, and Add puts it on the shelf. */
@Composable
fun PreviewScreen(app: AppState, p: Screen.Preview) {
    val scope = rememberCoroutineScope()
    val shows by app.library.shows.collectAsState()
    var info by remember(p) { mutableStateOf<FeedInfo?>(null) }
    var feedUrl by remember(p) { mutableStateOf(p.feedUrl) }
    var error by remember(p) { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    val compact = LocalCompact.current
    LaunchedEffect(p) {
        try {
            var url = p.feedUrl
            if (url.isBlank() && p.term.isNotBlank()) url = Apple.search(p.term, p.country, 10).firstOrNull { related(it.title, p.title) }?.feedUrl.orEmpty()
            if (url.isBlank()) url = p.site
            if (url.isBlank()) throw FetchException("Couldn’t find a public feed for ${p.title}.")
            feedUrl = url
            info = Peek.feed(url)
        } catch (e: Exception) {
            error = tr(e.message ?: "Couldn’t load episodes.")
        }
    }
    val onShelf = shows.firstOrNull { (feedUrl.isNotBlank() && it.feedUrl == feedUrl) || it.title.equals(p.title, ignoreCase = true) }
    val title = info?.title?.ifBlank { null } ?: p.title
    val image = info?.image?.ifBlank { null } ?: p.image
    val hue = remember(title) { hueOf(title) }
    val tint = Color.hsl(hue, 0.5f, if (look.dark) 0.24f else 0.86f)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = pad()) {
        item {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint, look.bg.copy(alpha = 0f))))) {
                Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 14.dp, bottom = 8.dp)) {
                    RoundButton(WebIcons.Chevleft, tr("Back"), { app.back() }, size = 32.dp)
                    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Artwork(image, title, Modifier.size(if (compact) 120.dp else 170.dp), corner = 12.dp)
                        Column(Modifier.padding(start = 24.dp).weight(1f)) {
                            Text(tr("Preview"), style = Type.meta, color = look.accent)
                            Text(title, style = Type.pageTitle.copy(fontSize = if (compact) 26.sp else 36.sp, lineHeight = if (compact) 30.sp else 40.sp), color = look.text,
                                maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                            info?.author?.takeIf { it.isNotBlank() }?.let { Text(it, style = Type.body, color = look.text2, modifier = Modifier.padding(top = 4.dp)) }
                            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (onShelf != null) Pill(tr("Open"), WebIcons.Stack, filled = true, big = true, onClick = { app.open(Screen.ShowPage(onShelf.sid)) })
                                else Pill(tr(if (adding) "Adding…" else "Add to library"), WebIcons.Plus, filled = true, big = true, onClick = {
                                    if (adding || feedUrl.isBlank()) return@Pill
                                    adding = true
                                    scope.launch {
                                        try {
                                            val s = app.library.addByLink(feedUrl)
                                            app.say(tr("Added ${s.title} to your library."))
                                            app.stack.removeAt(app.stack.lastIndex); app.open(Screen.ShowPage(s.sid))
                                        } catch (e: FetchException) { app.say(tr(e.message ?: "Couldn’t add that show.")) } finally { adding = false }
                                    }
                                })
                            }
                        }
                    }
                    info?.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = Type.epDesc, color = look.text2, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp).widthIn(max = 720.dp))
                    }
                }
            }
        }
        when {
            error != null -> item { Text(error!!, style = Type.body, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = gutter, vertical = 16.dp)) }
            info == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 20.dp).clip(CircleShape), color = look.accent, trackColor = look.fill2) }
            else -> {
                val list = info!!.episodes.take(80)
                item { SectionTitle(tr("Episodes"), tr("Play any of them before you decide.")) }
                items(list, key = { it.id }) { ep -> EpisodeRow(app, ep, list = list) }
            }
        }
    }
}

private val REGIONS = listOf("" to "Near you", "JP" to "日本", "CN" to "中国", "TW" to "台灣", "HK" to "香港", "KR" to "한국", "US" to "United States",
    "GB" to "United Kingdom", "FR" to "France", "DE" to "Deutschland", "IT" to "Italia", "ES" to "España", "CA" to "Canada", "AU" to "Australia")

/** Public FM and AM stations, live, by region: from Radio Browser, the free directory of radio streams. */
@Composable
fun RadioScreen(app: AppState) {
    val near = remember { app.platform.locale().split('-', '_').drop(1).firstOrNull { it.length == 2 }?.uppercase() ?: "US" }
    var region by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var term by remember { mutableStateOf("") }
    var stations by remember { mutableStateOf<List<Station>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val code = region.ifBlank { near }
    val android = app.platform.name == "Android"
    LaunchedEffect(code, term) {
        stations = null; error = null
        try {
            // the desktop player takes MP3 and AAC streams (and HLS); Android plays them all
            stations = Radio.stations(code, term).filter { android || it.hls == 1 || it.codec.uppercase() in setOf("MP3", "AAC", "AAC+", "") }
        } catch (e: Exception) { error = tr(e.message ?: "The radio directory didn’t answer.") }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = pad()) {
        item { PageTitle(tr("Radio"), tr("Public FM and AM stations, live from around the world.")) }
        item {
            LazyRow(Modifier.padding(top = 18.dp), contentPadding = PaddingValues(horizontal = gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(REGIONS) { (c, label) -> Chip(if (c.isEmpty()) tr(label) else label, region == c) { region = c } }
            }
        }
        item {
            SearchBox(query, { query = it }, tr("Find a station"), onSubmit = { term = query.trim() },
                modifier = Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 14.dp, bottom = 6.dp), height = 38.dp)
        }
        when {
            error != null -> item { Text(error!!, style = Type.body, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = gutter, vertical = 14.dp)) }
            stations == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 20.dp).clip(CircleShape), color = look.accent, trackColor = look.fill2) }
            stations!!.isEmpty() -> item { Text(tr("No stations found."), style = Type.body, color = look.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 14.dp)) }
            else -> items(stations!!, key = { it.stationuuid }) { s -> StationRow(app, s) }
        }
        item {
            Text(tr("Stations come from Radio Browser, a free directory of public radio streams."), style = Type.cardSub, color = look.text3,
                modifier = Modifier.padding(horizontal = gutter, vertical = 18.dp))
        }
    }
}

@Composable
private fun StationRow(app: AppState, s: Station) {
    val currentId by app.currentId.collectAsState()
    val playingNow by app.playingNow.collectAsState()
    val ep = remember(s.stationuuid) { Radio.episode(s) }
    val on = currentId == ep.id
    Column(Modifier.padding(horizontal = gutter)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { if (on) app.playback.toggle() else app.playback.play(ep, listOf(ep)) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Artwork(s.favicon, s.name, Modifier.size(52.dp), corner = 10.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(s.name.trim(), style = Type.epTitle, color = look.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val where = listOf(s.state.ifBlank { s.country }, s.tags.split(',').map { it.trim() }.filter { it.isNotBlank() }.take(2).joinToString(", "))
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (where.isNotBlank()) Text(where, style = Type.cardSub, color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(s.codec, if (s.bitrate > 0) "${s.bitrate} kbps" else "").filter { it.isNotBlank() }.joinToString(" · "), style = Type.meta, color = look.text3)
            }
            Pill(tr(if (on && playingNow) "Playing" else "Listen"), if (on && playingNow) WebIcons.Wave else WebIcons.Play, filled = on && playingNow,
                onClick = { if (on) app.playback.toggle() else app.playback.play(ep, listOf(ep)) })
        }
        Separator()
    }
}

/** Topic search results: single episodes, videos (when yt-dlp is installed) and shows. */
@Composable
fun TopicResult(app: AppState, h: EpisodeHit) {
    val ep = remember(h.url) { Topics.episode(h) }
    val currentId by app.currentId.collectAsState()
    val playingNow by app.playingNow.collectAsState()
    val on = currentId == ep.id
    Column(Modifier.padding(horizontal = gutter)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { app.open(Screen.EpisodePreview(ep, h.show, h.feedUrl)) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top) {
            Artwork(h.image, h.show, Modifier.size(64.dp), corner = 8.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(listOf(h.show, if (h.date > 0) fmtDate(h.date) else "").filter { it.isNotBlank() }.joinToString(" · "), style = Type.meta, color = look.accent, maxLines = 1)
                Text(h.title, style = Type.epTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                if (h.desc.isNotBlank()) Text(h.desc, style = Type.epDesc, color = look.text2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(if (on && playingNow) tr("Playing") else fmtLength(h.durationSec).ifBlank { tr("Play") }, if (on && playingNow) WebIcons.Wave else WebIcons.Play,
                        filled = on && playingNow, onClick = { if (on) app.playback.toggle() else app.playback.play(ep, listOf(ep)) })
                    if (h.feedUrl.isNotBlank()) Text(tr("Open show"), style = Type.pill, color = look.accent, modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .clickable { app.open(Screen.Preview(title = h.show, feedUrl = h.feedUrl, image = h.image)) }.padding(6.dp))
                }
            }
        }
        Separator()
    }
}

@Composable
fun VideoResult(app: AppState, v: VideoHit) {
    val ep = remember(v.url) { Video.episode(v) }
    val currentId by app.currentId.collectAsState()
    val playingNow by app.playingNow.collectAsState()
    val on = currentId == ep.id
    Column(Modifier.padding(horizontal = gutter)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { app.open(Screen.EpisodePreview(ep, v.channel)) }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Artwork(v.thumb, v.title, Modifier.width(112.dp).height(63.dp), corner = 8.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(v.title, style = Type.epTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOf(v.channel, v.site, fmtLength(v.durationSec)).filter { it.isNotBlank() }.joinToString(" · "), style = Type.cardSub, color = look.text2, maxLines = 1)
            }
            Pill(tr(if (on && playingNow) "Playing" else "Listen"), if (on && playingNow) WebIcons.Wave else WebIcons.Play, filled = on && playingNow,
                onClick = { if (on) app.playback.toggle() else app.playback.play(ep, listOf(ep)) })
        }
        Separator()
    }
}

/** One episode from a search: its cover, show and notes, to play now or open its show. */
@Composable
fun EpisodePreviewScreen(app: AppState, p: Screen.EpisodePreview) {
    val ep = p.ep
    val currentId by app.currentId.collectAsState()
    val playingNow by app.playingNow.collectAsState()
    val on = currentId == ep.id
    val compact = LocalCompact.current
    val name = p.show.ifBlank { ep.title }
    val hue = remember(name) { hueOf(name) }
    val tint = Color.hsl(hue, 0.5f, if (look.dark) 0.24f else 0.86f)
    val openShow = { app.open(Screen.Preview(title = p.show, feedUrl = p.feedUrl, image = ep.image)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = pad()) {
        item {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint, look.bg.copy(alpha = 0f))))) {
                Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 14.dp, bottom = 8.dp)) {
                    RoundButton(WebIcons.Chevleft, tr("Back"), { app.back() }, size = 32.dp)
                    val art = @Composable { Artwork(ep.image, name, Modifier.size(if (compact) 140.dp else 200.dp), corner = 12.dp) }
                    val texts = @Composable {
                        Text(tr("Episode"), style = Type.meta, color = look.accent)
                        if (p.show.isNotBlank()) Text(p.show, style = Type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = look.text2,
                            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).clickable(enabled = p.feedUrl.isNotBlank()) { openShow() })
                        Text(ep.title, style = Type.pageTitle.copy(fontSize = if (compact) 24.sp else 32.sp, lineHeight = if (compact) 28.sp else 37.sp), color = look.text,
                            maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        val meta = listOf(if (ep.date > 0) fmtDate(ep.date) else "", fmtLength(ep.duration)).filter { it.isNotBlank() }.joinToString(" · ")
                        if (meta.isNotBlank()) Text(meta, style = Type.meta, color = look.text3, modifier = Modifier.padding(top = 6.dp))
                        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(tr(if (on && playingNow) "Playing" else "Play"), if (on && playingNow) WebIcons.Wave else WebIcons.Play, filled = true, big = true,
                                onClick = { if (on) app.playback.toggle() else app.playback.play(ep, listOf(ep)) })
                            if (p.feedUrl.isNotBlank()) Pill(tr("Open show"), WebIcons.Stack, filled = false, big = true, onClick = { openShow() })
                        }
                    }
                    if (compact) {
                        Box(Modifier.padding(top = 14.dp, bottom = 16.dp)) { art() }
                        texts()
                    } else {
                        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            art()
                            Column(Modifier.padding(start = 28.dp).weight(1f)) { texts() }
                        }
                    }
                }
            }
        }
        if (ep.desc.isNotBlank()) {
            item { SectionTitle(tr("About this episode"), null) }
            item { Text(ep.desc, style = Type.pageSub, color = look.text2, modifier = Modifier.padding(horizontal = gutter).widthIn(max = 760.dp)) }
        }
    }
}
