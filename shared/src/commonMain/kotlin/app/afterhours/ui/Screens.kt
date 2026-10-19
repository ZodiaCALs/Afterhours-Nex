package app.afterhours.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.afterhours.core.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(app: AppState) {
    val shows by app.library.shows.collectAsState()
    val episodes by app.library.episodes.collectAsState()
    val progress by app.library.progress.collectAsState()
    val setup by app.library.setup.collectAsState()
    val upNext = remember(progress, episodes) { app.library.upNext() }
    val latest = remember(episodes) { app.library.latest() }
    val hour = (((Clock.now() + Clock.utcOffsetMinutes() * 60_000L) / 3_600_000L) % 24).toInt()
    val greeting = when { hour < 5 -> tr("Up late"); hour < 12 -> tr("Good morning"); hour < 18 -> tr("Good afternoon"); else -> tr("Good evening") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageTitle(tr("Listen Now"), tr("$greeting. Here’s what’s new on your shelf.")) }
        if (setup.running) item {
            InfoCard {
                Text(tr("Setting up your shelf"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(tr("Finding the ${setup.total} shows from your poster. ${setup.done} of ${setup.total} ready. Apple’s directory is queried gently, so this takes about a minute the first time."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                LinearProgressIndicator(progress = { if (setup.total > 0) setup.done.toFloat() / setup.total else 0f }, modifier = Modifier.fillMaxWidth())
                if (setup.note.isNotBlank()) Text(tr(setup.note), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (upNext.isNotEmpty()) {
            item { SectionTitle(tr("Up Next"), tr("Pick up where you left off.")) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(upNext, key = { it.id }) { ep -> UpNextCard(app, ep, progress[ep.id]) }
                }
            }
        }
        item { SectionTitle(tr("Latest episodes"), tr("New from the shows on your shelf.")) }
        if (latest.isEmpty()) item {
            Text(tr(if (shows.any { it.status == "pending" }) "Gathering the latest episodes…" else "No episodes yet. Open a show to load it."),
                Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(latest, key = { it.id }) { ep -> EpisodeRow(app, ep, showTitle = app.library.show(ep.sid)?.title, list = latest) }
    }
}

@Composable
private fun UpNextCard(app: AppState, ep: Episode, p: Progress?) {
    val show = app.library.show(ep.sid)
    Column(Modifier.width(160.dp).clip(RoundedCornerShape(12.dp)).clickable { app.playback.play(ep) }) {
        Artwork(ep.image.ifBlank { show?.image.orEmpty() }, show?.title ?: ep.title, Modifier.size(160.dp), corner = 12.dp)
        if (p != null && p.d > 0) LinearProgressIndicator(progress = { (p.t.toFloat() / p.d).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Text(ep.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(show?.title.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DiscoverScreen(app: AppState) {
    val shows by app.library.shows.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageTitle(tr("Discover"), tr("Related shows, picked around what you love.")) }
        for (cat in Catalog.data.categories) {
            val picks = (Catalog.data.extras + Catalog.data.presets).filter { it.cat == cat.id }
            if (picks.isEmpty()) continue
            item(key = "title-" + cat.id) { SectionTitle(cat.label, null) }
            item(key = "row-" + cat.id) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(picks, key = { it.key }) { pick -> PickCard(app, pick, shows) }
                }
            }
        }
    }
}

@Composable
private fun PickCard(app: AppState, pick: Pick, shows: List<Show>) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val onShelf = shows.firstOrNull { it.preset == pick.key || it.title.equals(pick.title, ignoreCase = true) }
    Column(Modifier.width(168.dp).clip(RoundedCornerShape(12.dp)).clickable(enabled = onShelf != null) { onShelf?.let { app.open(Screen.ShowPage(it.sid)) } }) {
        Artwork(onShelf?.image.orEmpty(), pick.title, Modifier.size(168.dp), corner = 12.dp)
        Text(pick.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(pick.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (onShelf != null) {
            Text(tr("On your shelf"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        } else {
            TextButton(enabled = !busy, onClick = {
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
            }) { Icon(Icons.Filled.Add, contentDescription = null); Text(tr(if (busy) "Adding…" else "Add")) }
        }
    }
}

@Composable
fun ShelfScreen(app: AppState) {
    val shows by app.library.shows.collectAsState()
    LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 24.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { PageTitle(tr("Library"), tr("${shows.size} shows")) }
        items(shows, key = { it.sid }) { s -> ShowTile(s) { app.open(Screen.ShowPage(s.sid)) } }
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
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text(tr("This show isn’t in your library"), style = MaterialTheme.typography.titleLarge)
            Text(tr("It may have been removed."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { app.tab(Screen.Shelf) }) { Text(tr("Open Library")) }
        }
        return
    }
    val list = remember(episodes, settings.sort, sid) { app.library.sorted(sid) }
    var limit by remember(sid) { mutableStateOf(60) }
    var changing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    LaunchedEffect(sid) { if (episodes[sid].isNullOrEmpty() && show.status == "ready") app.library.refreshNow(sid) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.padding(start = 4.dp, top = 4.dp)) {
                IconButton(onClick = { app.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back")) }
            }
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
                Artwork(show.image, show.title, Modifier.size(132.dp), corner = 14.dp)
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(show.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (show.author.isNotBlank()) Text(show.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (show.description.isNotBlank()) Text(show.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (list.isNotEmpty()) Button(onClick = { app.playback.play(list.first(), list) }) { Icon(Icons.Filled.PlayArrow, contentDescription = null); Text(tr("Latest episode")) }
                IconButton(onClick = { scope.launch { val err = app.library.refreshNow(sid); app.say(tr(err ?: "This show is up to date.")) } }) { Icon(Icons.Filled.Refresh, contentDescription = tr("Check for new episodes")) }
                val site = show.link.ifBlank { show.pageUrl }
                if (site.startsWith("http")) IconButton(onClick = { app.platform.openUrl(site) }) { Icon(Icons.Filled.Public, contentDescription = tr("Website")) }
                TextButton(onClick = { changing = true }) { Text(tr("Change source")) }
                IconButton(onClick = { removing = true }) { Icon(Icons.Filled.Delete, contentDescription = tr("Remove from library")) }
            }
        }
        item {
            when {
                sid in loading -> Column(Modifier.padding(16.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(tr(if (show.source == "web") "Looking through the show’s website…" else "Finding this show’s feed…"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
                errors[sid] != null -> Text(tr("Couldn’t load episodes. ${errors[sid]}"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                show.status == "missing" -> Text(tr("Afterhours couldn’t find this show’s feed in Apple’s directory or on its website. Use Change source to point it at one."),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                show.source == "web" -> Text(tr("No working RSS feed, so these episodes come from the show’s website."), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Episodes"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { app.library.updateSettings { it.copy(sort = if (it.sort == "oldest") "newest" else "oldest") } }) {
                    Text(tr(if (settings.sort == "oldest") "Oldest first" else "Newest first"))
                }
            }
        }
        items(list.take(limit), key = { it.id }) { ep -> EpisodeRow(app, ep, list = list) }
        if (list.size > limit) item {
            TextButton(onClick = { limit += 100 }, modifier = Modifier.padding(horizontal = 8.dp)) { Text(tr("Show more episodes (${list.size - limit})")) }
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
                    Text(tr("Paste the show’s RSS feed or website. If there isn’t a feed, Afterhours looks through the website for episodes."), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = true, placeholder = { Text(tr("RSS feed or website address")) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
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
            title = { Text(tr("Remove “${show.title}” from your library?")) },
            confirmButton = { TextButton(onClick = { removing = false; app.library.remove(sid); app.back(); app.say(tr("Removed ${show.title}.")) }) { Text(tr("Remove")) } },
            dismissButton = { TextButton(onClick = { removing = false }) { Text(tr("Cancel")) } },
        )
    }
}

@Composable
fun SearchScreen(app: AppState) {
    val scope = rememberCoroutineScope()
    val shows by app.library.shows.collectAsState()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AppleShow>>(emptyList()) }
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
                    if (results.isEmpty()) error = tr("No shows found. Try fewer words, or paste the feed link.")
                }
            } catch (e: FetchException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PageTitle(tr("Search"), null) }
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, placeholder = { Text(tr("Shows, topics, or a feed link")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { run(query) }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp)) }
        error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) } }
        if (results.isEmpty() && !busy) {
            item { SectionTitle(tr("Try a topic"), null) }
            items(Catalog.data.categories.flatMap { c -> c.keywords.take(1).map { it.first() } }) { term ->
                Text(term, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().clickable { query = term; run(term) }.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
        items(results, key = { it.feedUrl }) { r ->
            val on = shows.firstOrNull { it.feedUrl == r.feedUrl }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(r.artwork, r.title, Modifier.size(56.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(r.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(tr(listOf(r.artist, r.genre).filter { it.isNotBlank() }.joinToString(" · ")), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (on != null) TextButton(onClick = { app.open(Screen.ShowPage(on.sid)) }) { Text(tr("Open")) }
                else FilledTonalButton(onClick = { val s = app.library.addFromApple(r); app.say(tr("Added ${s.title} to your library.")) }) { Text(tr("Add")) }
            }
        }
    }
}

@Composable
fun SettingsScreen(app: AppState) {
    val scope = rememberCoroutineScope()
    val settings by app.library.settings.collectAsState()
    val shows by app.library.shows.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        PageTitle(tr("Settings"), null)
        LanguageSettings(app)
        SectionTitle(tr("Appearance"), null)
        Box(Modifier.padding(horizontal = 16.dp)) { Choice(listOf("system" to tr("System"), "light" to tr("Light"), "dark" to tr("Dark")), settings.theme) { v -> app.library.updateSettings { it.copy(theme = v) } } }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Opening animation"), modifier = Modifier.weight(1f))
            Switch(checked = settings.intro, onCheckedChange = { on -> app.library.updateSettings { it.copy(intro = on) } })
        }
        SectionTitle(tr("Playback"), null)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Play the next episode automatically"), modifier = Modifier.weight(1f))
            Switch(checked = settings.autoNext, onCheckedChange = { on -> app.library.updateSettings { it.copy(autoNext = on) } })
        }
        Text(tr("Speed"), modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
        Box(Modifier.padding(horizontal = 16.dp)) {
            Choice(listOf("0.8", "1.0", "1.2", "1.5", "2.0").map { it to "$it×" }, settings.speed.toString().take(3)) { v -> app.playback.speed(v.toFloat()) }
        }
        CaptionSettings(app)
        PhoneSettings(app)
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
