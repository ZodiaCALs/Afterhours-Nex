package app.afterhours.ui

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.afterhours.core.*
import kotlinx.coroutines.launch

@Composable
private fun bottomPadding() = PaddingValues(bottom = 32.dp + LocalBottomSpace.current)

@Composable
fun HomeScreen(app: AppState) {
    val shows by app.library.shows.collectAsState()
    val episodes by app.library.episodes.collectAsState()
    val progress by app.library.progress.collectAsState()
    val setup by app.library.setup.collectAsState()
    val upNext = remember(progress, episodes) { app.library.upNext() }
    val latest = remember(episodes) { app.library.latest().take(24) }
    val picks = remember(shows) { Catalog.data.extras.filter { p -> shows.none { it.preset == p.key || it.title.equals(p.title, ignoreCase = true) } }.take(14) }
    val hour = (((Clock.now() + Clock.utcOffsetMinutes() * 60_000L) / 3_600_000L) % 24).toInt()
    val greeting = when { hour < 5 -> tr("Up late"); hour < 12 -> tr("Good morning"); hour < 18 -> tr("Good afternoon"); else -> tr("Good evening") }
    val compact = LocalCompact.current
    val update by app.update.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding()) {
        item { PageTitle(tr("Listen Now"), tr("$greeting. Here’s what’s new on your shelf.")) }
        update?.let { r ->
            item {
                InfoCard(Modifier.padding(top = 14.dp)) {
                    Text(tr("AfterhoursNex ${r.version} is available"), style = Type.epTitle, color = look.text)
                    Text(tr("A new release is on GitHub."), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(tr("Download"), WebIcons.Export, filled = true, onClick = { app.platform.openUrl(r.download) })
                        Pill(tr("What’s new"), null, filled = false, onClick = { app.platform.openUrl(r.page) })
                        Pill(tr("Not now"), null, filled = false, onClick = { app.skipUpdate() })
                    }
                }
            }
        }
        if (compact) item {
            Row(Modifier.padding(start = gutter, top = 14.dp)) { RoundButton(WebIcons.Gear, tr("Settings"), { app.tab(Screen.Prefs) }, size = 38.dp) }
        }
        if (setup.running) item {
            InfoCard(Modifier.padding(top = 14.dp)) {
                Text(tr("Setting up your shelf"), style = Type.epTitle, color = look.text)
                Text(tr("Finding the ${setup.total} shows from your poster. ${setup.done} of ${setup.total} ready. Apple’s directory is queried gently, so this takes about a minute the first time."),
                    style = Type.epDesc, color = look.text2, modifier = Modifier.padding(vertical = 8.dp))
                LinearProgressIndicator(progress = { if (setup.total > 0) setup.done.toFloat() / setup.total else 0f }, modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = look.accent, trackColor = look.fill2)
                if (setup.note.isNotBlank()) Text(tr(setup.note), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (upNext.isNotEmpty()) item {
            CardRow(tr("Up Next"), tr("Pick up where you left off.")) { items(upNext, key = { it.id }) { ep -> UpNextCard(app, ep, progress[ep.id]) } }
        }
        item { SectionTitle(tr("Latest episodes"), tr("New from the shows on your shelf.")) }
        if (latest.isEmpty()) item {
            Text(tr(if (shows.any { it.status == "pending" }) "Gathering the latest episodes…" else "No episodes yet. Open a show to load it."),
                Modifier.padding(horizontal = gutter, vertical = 8.dp), style = Type.body, color = look.text2)
        }
        items(latest, key = { it.id }) { ep -> EpisodeRow(app, ep, showTitle = app.library.show(ep.sid)?.name, list = latest, modifier = Modifier.animateItem()) }
        if (picks.isNotEmpty()) item {
            CardRow(tr("Picked for you"), tr("Related shows that sit well next to your shelf."), onSeeAll = { app.tab(Screen.Discover) }) {
                items(picks, key = { it.key }) { pick -> PickCard(app, pick, shows) }
            }
        }
    }
}

@Composable
private fun UpNextCard(app: AppState, ep: Episode, p: Progress?) {
    val show = app.library.show(ep.sid)
    val ia = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(Modifier.width(174.dp).pressable(ia).clip(RoundedCornerShape(12.dp)).hoverFill(ia, RoundedCornerShape(12.dp), look.fill.copy(alpha = 0.5f))
        .clickable(ia, null) { app.playback.play(ep) }.padding(6.dp)) {
        Artwork(ep.image.ifBlank { show?.art.orEmpty() }, show?.name ?: ep.title, Modifier.fillMaxWidth().aspectRatio(1f), corner = 10.dp)
        if (p != null && p.d > 0) {
            Box(Modifier.padding(top = 9.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(look.fill2)) {
                Box(Modifier.fillMaxWidth((p.t.toFloat() / p.d).coerceIn(0f, 1f)).fillMaxHeight().background(look.accent))
            }
        }
        Text(ep.title, style = Type.cardTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(show?.name.orEmpty(), style = Type.cardSub, color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun DiscoverScreen(app: AppState) {
    val shows by app.library.shows.collectAsState()
    var cat by remember { mutableStateOf("") }
    val compact = LocalCompact.current
    val cats = Catalog.data.categories
    val all = remember { Catalog.data.extras + Catalog.data.presets }
    val liked = remember(shows) { cats.filter { c -> shows.any { s -> Catalog.data.presets.any { it.key == s.preset && it.cat == c.id } } }.map { it.id }.toSet() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding()) {
        item { PageTitle(tr("Discover"), tr("Related shows, picked around what you love.")) }
        item {
            LazyRow(Modifier.padding(top = 18.dp, bottom = 6.dp), contentPadding = PaddingValues(horizontal = gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(tr("For you"), cat == "") { cat = "" } }
                items(cats, key = { it.id }) { c -> Chip(tr(c.label), cat == c.id) { cat = c.id } }
            }
        }
        if (cat == "") {
            item { Hero(shows, compact) }
            for (c in cats.sortedByDescending { it.id in liked }) {
                val picks = all.filter { it.cat == c.id }
                if (picks.isEmpty()) continue
                item(key = "row-" + c.id) {
                    if (c.id in liked) CardRow(tr("Because you like ${c.label}"), tr("From the interests you told me about."), onSeeAll = { cat = c.id }) {
                        items(picks, key = { it.key }) { pick -> PickCard(app, pick, shows) }
                    } else CardRow(tr("More ${c.label}"), null, onSeeAll = { cat = c.id }) {
                        items(picks, key = { it.key }) { pick -> PickCard(app, pick, shows) }
                    }
                }
            }
        } else {
            val picks = all.filter { it.cat == cat }
            val cols = if (compact) 2 else 5
            item { SectionTitle(tr(cats.firstOrNull { it.id == cat }?.label.orEmpty()), null) }
            items(picks.chunked(cols)) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = gutter - 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { pick -> PickCard(app, pick, shows, Modifier.weight(1f)) }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The banner at the top of Discover, with three of your shows fanned out on the right. */
@Composable
private fun Hero(shows: List<Show>, compact: Boolean) {
    Box(Modifier.padding(horizontal = gutter, vertical = 14.dp).fillMaxWidth().heightIn(min = if (compact) 0.dp else 220.dp).clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF34277E), Color(0xFF7A3496), Color(0xFFD9467A), Color(0xFFF7926D))))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = if (compact) 22.dp else 34.dp, vertical = if (compact) 22.dp else 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Made for your evenings"), style = Type.meta, color = Color.White.copy(alpha = 0.8f))
                Text(tr("City pop, slow jazz, good books and big ideas"), color = Color.White, modifier = Modifier.padding(top = 8.dp),
                    style = Type.pageTitle.copy(fontSize = if (compact) 24.sp else 30.sp, lineHeight = if (compact) 28.sp else 34.sp))
                Text(tr("Related shows for everything on your shelf. Press + to add one, or open it to preview its episodes first."),
                    style = Type.body, color = Color.White.copy(alpha = 0.86f), modifier = Modifier.padding(top = 10.dp).widthIn(max = 520.dp))
            }
            if (!compact) {
                val fan = shows.filter { it.image.isNotBlank() }.ifEmpty { shows }.take(3)
                Box(Modifier.width(270.dp).height(160.dp), contentAlignment = Alignment.Center) {
                    fan.forEachIndexed { i, s ->
                        val k = i - 1
                        Artwork(s.image, s.title, Modifier.zIndex(if (k == 0) 2f else 1f).offset(x = (k * 72).dp, y = if (k == 0) (-6).dp else 8.dp).rotate(k * 9f).size(124.dp), corner = 12.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickCard(app: AppState, pick: Pick, shows: List<Show>, modifier: Modifier = Modifier.width(174.dp)) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val onShelf = shows.firstOrNull { it.preset == pick.key || it.title.equals(pick.title, ignoreCase = true) }
    val ia = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(modifier.pressable(ia).clip(RoundedCornerShape(12.dp)).hoverFill(ia, RoundedCornerShape(12.dp), look.fill.copy(alpha = 0.5f))
        .clickable(ia, null) {
            if (onShelf != null) app.open(Screen.ShowPage(onShelf.sid))
            else app.open(Screen.Preview(title = pick.title, term = pick.term.ifBlank { pick.title }, country = pick.countries.firstOrNull() ?: "us", site = pick.site))
        }.padding(6.dp)) {
        var cover by remember(pick.key) { mutableStateOf(onShelf?.image?.ifBlank { null } ?: Covers.cached(pick.key)) }
        LaunchedEffect(pick.key) { if (cover.isBlank()) cover = Covers.find(pick) }      // only for cards on screen; remembered after
        Artwork(cover, pick.title, Modifier.fillMaxWidth().aspectRatio(1f), corner = 10.dp)
        Text(pick.title, style = Type.cardTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
        Text(pick.why, style = Type.cardSub, color = look.text2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        if (onShelf != null) {
            Text(tr("On your shelf"), style = Type.meta, color = look.accent, modifier = Modifier.padding(top = 10.dp))
        } else {
            Pill(tr(if (busy) "Adding…" else "Add"), WebIcons.Plus, filled = false, modifier = Modifier.padding(top = 10.dp), onClick = {
                if (busy) return@Pill
                busy = true
                scope.launch {
                    try {
                        val found = Apple.search(pick.term.ifBlank { pick.title }, pick.countries.firstOrNull() ?: "us", 10).firstOrNull { related(it.title, pick.title) }
                        val show = when {
                            found != null -> app.library.addFromApple(found)
                            pick.site.isNotBlank() -> app.library.addByLink(pick.site)
                            else -> throw FetchException("Couldn’t find a public feed for ${pick.title}.")
                        }
                        app.say(tr("Added ${show.title} to your library."))
                    } catch (e: FetchException) {
                        app.say(tr(e.message ?: "Couldn’t add that show."))
                    } finally {
                        busy = false
                    }
                }
            })
        }
    }
}

@Composable
fun ShelfScreen(app: AppState) {
    val shows by app.library.shows.collectAsState()
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    val compact = LocalCompact.current
    val g = gutter
    fun add() {
        val url = link.trim()
        if (!url.startsWith("http") || adding) return
        adding = true
        scope.launch {
            try {
                val s = app.library.addByLink(url)
                link = ""
                app.say(tr("Added ${s.title} to your library."))
                app.open(Screen.ShowPage(s.sid))
            } catch (e: FetchException) {
                app.say(tr(e.message ?: "Couldn’t add that show."))
            } finally {
                adding = false
            }
        }
    }
    fun importAudio() {
        scope.launch {
            val files = app.platform.openAudio()
            if (files.isEmpty()) return@launch
            val s = app.library.addLocal(files)
            app.say(tr("Imported ${files.size} audio files."))
            app.open(Screen.ShowPage(s.sid))
        }
    }
    val catOf = remember { Catalog.data.presets.associate { it.key to it.cat } }
    val groups = remember(shows) {
        val mine = shows.filter { it.group.isNotBlank() }.groupBy { it.group }.map { (g, list) -> g to list }       // the listener's groups first
        val byCat = shows.filter { it.group.isBlank() }.groupBy { catOf[it.preset].orEmpty() }
        mine + Catalog.data.categories.mapNotNull { c -> byCat[c.id]?.let { tr(c.label) to it } } + listOfNotNull(byCat[""]?.let { tr("Added by you") to it })
    }
    val addRow = @Composable { m: Modifier ->
        Row(m, verticalAlignment = Alignment.CenterVertically) {
            SearchBox(link, { link = it }, tr("Paste a feed or Apple Podcasts link"), onSubmit = { add() }, modifier = Modifier.weight(1f), icon = null, height = 34.dp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.height(34.dp).clip(RoundedCornerShape(9.dp)).background(look.fill2).clickable(enabled = !adding) { add() }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center) {
                Text(tr(if (adding) "Adding…" else "Add"), style = Type.pill.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = look.text)
            }
            Spacer(Modifier.width(8.dp))
            Pill(tr("Import audio"), WebIcons.Import, filled = false, onClick = { importAudio() }, modifier = Modifier.height(34.dp))
        }
    }
    LazyVerticalGrid(GridCells.Adaptive(if (compact) 150.dp else 170.dp), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = g - 6.dp, end = g - 6.dp, bottom = 32.dp + LocalBottomSpace.current)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CompositionLocalProvider(LocalGutter provides 6.dp) {
                Column {
                    if (compact) {
                        PageTitle(tr("Library"), tr("${shows.size} shows"))
                        addRow(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 14.dp))
                    } else PageTitle(tr("Library"), tr("${shows.size} shows")) { addRow(Modifier.width(380.dp)) }
                }
            }
        }
        for ((label, list) in groups) {
            item(key = "h-$label", span = { GridItemSpan(maxLineSpan) }) { CompositionLocalProvider(LocalGutter provides 6.dp) { SectionTitle(label, null) } }
            items(list, key = { it.sid }) { s ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    ShowTile(s, onMenu = { menu = true }) { app.open(Screen.ShowPage(s.sid)) }
                    ShowMenu(app, s, menu) { menu = false }
                }
            }
        }
    }
}

@Composable
fun ShowScreen(app: AppState, sid: String) {
    val scope = rememberCoroutineScope()
    val shows by app.library.shows.collectAsState()
    val episodes by app.library.episodes.collectAsState()
    val settings by app.library.settings.collectAsState()
    val loading by app.library.loading.collectAsState()
    val errors by app.library.errors.collectAsState()
    val show = shows.firstOrNull { it.sid == sid }
    if (show == null) {
        Column(Modifier.fillMaxSize().padding(horizontal = gutter, vertical = 32.dp)) {
            Text(tr("This show isn’t in your library"), style = Type.sectionTitle, color = look.text)
            Text(tr("It may have been removed."), style = Type.body, color = look.text2, modifier = Modifier.padding(top = 6.dp))
            Pill(tr("Open Library"), null, filled = true, onClick = { app.tab(Screen.Shelf) }, modifier = Modifier.padding(top = 16.dp))
        }
        return
    }
    val list = remember(episodes, settings.sort, sid) { app.library.sorted(sid) }
    var limit by remember(sid) { mutableStateOf(60) }
    var changing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val compact = LocalCompact.current
    val dark = look.dark
    val hue = remember(show.title) { hueOf(show.title) }
    val tint = Color.hsl(hue, 0.5f, if (dark) 0.26f else 0.84f)
    val authorColor = Color.hsl(hue, if (dark) 0.82f else 0.6f, if (dark) 0.74f else 0.40f)
    val category = remember(show.preset) {
        Catalog.data.presets.firstOrNull { it.key == show.preset }?.cat?.let { c -> Catalog.data.categories.firstOrNull { it.id == c }?.label }
    }
    val site = show.link.ifBlank { show.pageUrl }
    LaunchedEffect(sid) { if (episodes[sid].isNullOrEmpty() && show.status == "ready") app.library.refreshNow(sid) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding()) {
        item {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint, look.bg)))) {
                Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 14.dp, bottom = 8.dp)) {
                    RoundButton(WebIcons.Chevleft, tr("Back"), { app.back() }, size = 32.dp)
                    val texts = @Composable {
                        if (category != null) Text(tr(category), style = Type.meta, color = look.text2)
                        Text(show.name, style = Type.pageTitle.copy(fontSize = if (compact) 30.sp else 40.sp, lineHeight = if (compact) 34.sp else 44.sp),
                            color = look.text, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        if (show.author.isNotBlank()) Text(show.author, style = Type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = authorColor,
                            modifier = Modifier.padding(top = 6.dp).clickable(enabled = site.startsWith("http")) { app.platform.openUrl(site) })
                        if (show.description.isNotBlank()) Text(show.description, style = Type.epDesc, color = look.text2, maxLines = 3, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp).widthIn(max = 620.dp))
                        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (list.isNotEmpty()) Pill(tr("Latest episode"), WebIcons.Play, filled = true, big = true, onClick = { app.playback.play(list.first(), list) })
                            if (show.source == "local") RoundButton(WebIcons.Plus, tr("Import audio files"), {
                                scope.launch { val files = app.platform.openAudio(); if (files.isNotEmpty()) { app.library.addLocal(files); app.say(tr("Imported ${files.size} audio files.")) } }
                            }) else {
                                RoundButton(WebIcons.Refresh, tr("Check for new episodes"), { scope.launch { val err = app.library.refreshNow(sid); app.say(tr(err ?: "This show is up to date.")) } })
                                RoundButton(WebIcons.Swap, tr("Change source"), { changing = true })
                            }
                            if (site.startsWith("http")) RoundButton(WebIcons.Link, tr("Website"), { app.platform.openUrl(site) })
                            RoundButton(WebIcons.Trash, tr("Remove from library"), { removing = true })
                        }
                    }
                    if (compact) {
                        ShowArt(show, Modifier.padding(top = 14.dp, bottom = 16.dp).size(150.dp), corner = 12.dp)
                        texts()
                    } else {
                        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShowArt(show, Modifier.size(190.dp), corner = 12.dp)
                            Column(Modifier.padding(start = 28.dp).weight(1f)) { texts() }
                        }
                    }
                }
            }
        }
        item {
            val note: Pair<String, Color>? = when {
                errors[sid] != null -> tr("Couldn’t load episodes. ${errors[sid]}") to MaterialTheme.colorScheme.error
                show.status == "missing" -> tr("Afterhours couldn’t find this show’s feed in Apple’s directory or on its website. Use Change source to point it at one.") to look.text2
                show.source == "web" -> tr("No working RSS feed, so these episodes come from the show’s website.") to look.text2
                else -> null
            }
            if (sid in loading) Column(Modifier.padding(horizontal = gutter, vertical = 12.dp)) {
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape), color = look.accent, trackColor = look.fill2)
                Text(tr(if (show.source == "web") "Looking through the show’s website…" else "Finding this show’s feed…"), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(top = 8.dp))
            } else if (note != null) Text(note.first, style = Type.cardSub, color = note.second, modifier = Modifier.padding(horizontal = gutter, vertical = 12.dp))
        }
        item {
            SectionTitle(tr("Episodes"), null) {
                Row(Modifier.height(30.dp).glass(CircleShape)
                    .clickable { app.library.updateSettings { it.copy(sort = if (it.sort == "oldest") "newest" else "oldest") } }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(WebIcons.Sort, contentDescription = null, tint = look.text, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(tr(if (settings.sort == "oldest") "Oldest first" else "Newest first"), style = Type.pill, color = look.text)
                }
            }
        }
        items(list.take(limit), key = { it.id }) { ep -> EpisodeRow(app, ep, list = list, modifier = Modifier.animateItem()) }
        if (list.size > limit) item {
            Text(tr("Show more episodes (${list.size - limit})"), style = Type.body, color = look.accent,
                modifier = Modifier.padding(horizontal = gutter, vertical = 14.dp).clip(RoundedCornerShape(6.dp)).clickable { limit += 100 }.padding(4.dp))
        }
    }
    if (changing) {
        var url by remember { mutableStateOf("") }
        var working by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!working) changing = false },
            title = { Text(tr("Change source")) },
            text = {
                Column {
                    Text(tr("Paste the show’s RSS feed or website. If there isn’t a feed, Afterhours looks through the website for episodes."), style = Type.cardSub)
                    SearchBox(url, { url = it }, tr("RSS feed or website address"), onSubmit = {}, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), icon = null, height = 36.dp)
                }
            },
            confirmButton = {
                TextButton(enabled = !working && url.startsWith("http"), onClick = {
                    working = true
                    scope.launch {
                        val err = app.library.changeSource(sid, url)
                        working = false
                        if (err == null) { changing = false; app.say(tr("${show.title} now has a new source.")) } else app.say(err)
                    }
                }) { Text(tr(if (working) "Checking…" else "Use this source")) }
            },
            dismissButton = { TextButton(enabled = !working, onClick = { changing = false }) { Text(tr("Cancel")) } },
        )
    }
    if (removing) {
        AlertDialog(
            onDismissRequest = { removing = false },
            title = { Text(tr("Remove “${show.name}” from your library?")) },
            confirmButton = { TextButton(onClick = {
                removing = false; app.library.remove(sid); app.back()
                app.sayUndo(tr("Removed ${show.name}.")) { app.library.undoRemove()?.let { app.say(tr("${it.name} is back.")) } }
            }) { Text(tr("Remove")) } },
            dismissButton = { TextButton(onClick = { removing = false }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
fun SearchScreen(app: AppState) {
    val scope = rememberCoroutineScope()
    val shows by app.library.shows.collectAsState()
    var query by remember { mutableStateOf(app.searchTerm) }
    var topics by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<AppleShow>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<EpisodeHit>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoHit>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun run(q: String) {
        val term = q.trim()
        if (term.isEmpty() || busy) return
        busy = true; error = null
        scope.launch {
            try {
                if (term.startsWith("http://") || term.startsWith("https://")) {
                    val s = app.library.addByLink(term)
                    app.say(tr("Added ${s.title} to your library."))
                    app.open(Screen.ShowPage(s.sid))
                } else {
                    results = Apple.search(term, "us", 25).distinctBy { it.feedUrl }
                    episodes = if (topics) runCatching { Topics.episodes(term) }.getOrDefault(emptyList()) else emptyList()
                    videos = if (topics) Video.bridge?.takeIf { it.available }?.let { b -> runCatching { b.search(term, 6) }.getOrDefault(emptyList()) }.orEmpty() else emptyList()
                    if (results.isEmpty() && episodes.isEmpty() && videos.isEmpty()) error = tr("No shows found. Try fewer words, or paste the feed link.")
                }
            } catch (e: FetchException) {
                error = tr(e.message ?: "Couldn’t add that show.")
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(app.searchTerm) { if (app.searchTerm.isNotBlank()) { query = app.searchTerm; run(app.searchTerm) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = bottomPadding()) {
        item { PageTitle(tr("Search"), tr("Apple’s podcast directory, or any feed or website.")) }
        item {
            SearchBox(query, { query = it }, tr(if (topics) "A topic, such as cyberpunk edgerunners review" else "Shows, topics, or a feed link"), onSubmit = { run(query) },
                modifier = Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 18.dp), height = 40.dp)
        }
        item {
            Row(Modifier.padding(start = gutter, end = gutter, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(tr("Shows"), !topics) { if (topics) { topics = false; run(query) } }
                Chip(tr("Episodes & topics"), topics) { if (!topics) { topics = true; run(query) } }
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 14.dp).clip(CircleShape), color = look.accent, trackColor = look.fill2) }
        error?.let { e -> item { Text(e, style = Type.body, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = gutter, vertical = 14.dp)) } }
        if (results.isEmpty() && episodes.isEmpty() && !busy) {
            item { SectionTitle(tr("Try a topic"), null) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Catalog.data.categories.flatMap { c -> c.keywords.take(1).map { it.first() } }) { term -> Chip(term, false) { query = term; run(term) } }
                }
            }
        }
        if (episodes.isNotEmpty()) {
            item { SectionTitle(tr("Episodes"), tr("Single episodes about your topic, best matches first.")) }
            items(episodes.take(25), key = { "e" + it.url }) { h -> TopicResult(app, h) }
        }
        val bridge = Video.bridge
        if (topics && episodes.isNotEmpty() && bridge != null && bridge.manageable && !bridge.available) {
            item { InfoCard(Modifier.padding(top = 8.dp)) { Text(tr("Videos about this, too"), style = Type.epTitle, color = look.text); VideoTools(app, Modifier.padding(top = 6.dp)) } }
        }
        if (videos.isNotEmpty()) {
            item { SectionTitle(tr("Videos"), tr("Played as audio.")) }
            items(videos, key = { "v" + it.url }) { v -> VideoResult(app, v) }
        }
        if (results.isNotEmpty()) item { SectionTitle(tr("Shows"), null) }
        items(if (topics) results.take(8) else results, key = { it.feedUrl }) { r ->
            val on = shows.firstOrNull { it.feedUrl == r.feedUrl }
            Column(Modifier.padding(horizontal = gutter)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable { if (on != null) app.open(Screen.ShowPage(on.sid)) else app.open(Screen.Preview(title = r.title, feedUrl = r.feedUrl, image = r.artwork)) }
                    .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(r.artwork, r.title, Modifier.size(56.dp), corner = 8.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(r.title, style = Type.epTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(tr(listOf(r.artist, r.genre).filter { it.isNotBlank() }.joinToString(" · ")), style = Type.cardSub, color = look.text2,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (on != null) Pill(tr("Open"), null, filled = false, onClick = { app.open(Screen.ShowPage(on.sid)) })
                    else Pill(tr("Add"), WebIcons.Plus, filled = false, onClick = { val s = app.library.addFromApple(r); app.say(tr("Added ${s.title} to your library.")) })
                }
                Separator()
            }
        }
    }
}

@Composable
fun SettingsScreen(app: AppState) {
    val scope = rememberCoroutineScope()
    val settings by app.library.settings.collectAsState()
    val shows by app.library.shows.collectAsState()
    val outer = (gutter - 16.dp).coerceAtLeast(0.dp)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = outer, end = outer, bottom = 32.dp + LocalBottomSpace.current)) {
      CompositionLocalProvider(LocalGutter provides 16.dp) {
        PageTitle(tr("Settings"), null)
        LanguageSettings(app)
        SectionTitle(tr("Appearance"), null)
        Box(Modifier.padding(horizontal = 16.dp)) { Choice(listOf("system" to tr("System"), "light" to tr("Light"), "dark" to tr("Dark")), settings.theme) { v -> app.library.updateSettings { it.copy(theme = v) } } }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Transparency"), style = Type.body, modifier = Modifier.weight(1f))
            Text("${(settings.glass * 100).roundToInt()}%", style = Type.cardSub, color = look.text2)
        }
        Slider(value = settings.glass, onValueChange = { v -> app.library.updateSettings { it.copy(glass = v) } }, valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = look.accent, inactiveTrackColor = look.fill2))
        Text(tr("How much the sidebar, player and tab bar let the page show through. 0% is solid."), style = Type.cardSub, color = look.text2,
            modifier = Modifier.padding(horizontal = 16.dp))
        SwitchRow(tr("Reduce effects"), tr("No blurred cover behind Now Playing and no glow behind the page: less memory, and smoother on older computers."),
            settings.lessEffects) { on -> app.library.updateSettings { it.copy(lessEffects = on) } }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Opening animation"), modifier = Modifier.weight(1f))
            Toggle(checked = settings.intro, onCheckedChange = { on -> app.library.updateSettings { it.copy(intro = on) } })
        }
        SectionTitle(tr("Playback"), null)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Play the next episode automatically"), modifier = Modifier.weight(1f))
            Toggle(checked = settings.autoNext, onCheckedChange = { on -> app.library.updateSettings { it.copy(autoNext = on) } })
        }
        Text(tr("Speed"), modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SPEED_CHOICES) { v -> Chip(speedLabel(v), kotlin.math.abs(settings.speed - v) < 0.01f) { app.playback.speed(v) } }
        }
        CaptionSettings(app)
        TranslateSettings(app)
        ModelSettings(app)
        PhoneSettings(app)
        CompanionSettings(app)
        StorageSettings(app)
        UpdateSettings(app)
        SectionTitle(tr("Library"), null)
        val missing = shows.count { it.status == "missing" }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr(if (missing > 0) "$missing show${if (missing > 1) "s" else ""} couldn’t be found automatically." else "Every show on your shelf was found."), modifier = Modifier.weight(1f))
            TextButton(enabled = missing > 0, onClick = { val n = app.library.retryMissing(); app.say(tr(if (n > 0) "Looking for $n show${if (n > 1) "s" else ""} again…" else "Nothing is missing.")) }) { Text(tr("Look again")) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Import OPML"))
                Text(tr("Add shows from another podcast app."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = {
                scope.launch {
                    val f = app.platform.openFile(listOf("opml", "xml")) ?: return@launch
                    val n = app.library.importOpml(f.bytes.decodeToString())
                    app.say(tr(if (n > 0) "Imported $n show${if (n > 1) "s" else ""}." else "No new shows in that file."))
                }
            }) { Text(tr("Import…")) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Export OPML"))
                Text(tr("Save your shows for another podcast app."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = {
                scope.launch { if (app.platform.saveFile("AfterhoursNex.opml", app.library.exportOpml().encodeToByteArray())) app.say(tr("Saved your shows.")) }
            }) { Text(tr("Export…")) }
        }
        SectionTitle(tr("About"), null)
        Text(tr("${AppInfo.NAME} ${AppInfo.VERSION} for ${app.platform.name}."), modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
}
