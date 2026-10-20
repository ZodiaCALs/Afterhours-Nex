package app.afterhours.ui

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.afterhours.core.*
import coil3.compose.LocalPlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val TRANSLATE_TO = listOf("auto" to "Interface language", "en" to "English", "zh-Hans" to "简体中文", "ja" to "日本語", "ko" to "한국어",
    "es" to "Español", "fr" to "Français", "de" to "Deutsch")

/** Settings › Caption translation. */
@Composable
fun TranslateSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    SectionTitle(tr("Caption translation"), null)
    SwitchRow(tr("Translate captions"), tr("Shows each caption line in your language too. The lines are sent to MyMemory, a free translation service; nothing else leaves your device."),
        s.translate) { on -> app.library.updateSettings { it.copy(translate = on) } }
    TranslationManager(app)
    if (!s.translate) return
    Text(tr("Translate into"), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TRANSLATE_TO) { (code, label) -> Chip(if (code == "auto") tr(label) else label, s.translateTo == code) { app.library.updateSettings { it.copy(translateTo = code) } } }
    }
    if (s.pro) {
        Text(tr("Translation service"), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp))
        Box(Modifier.padding(horizontal = 16.dp)) {
            Choice(listOf("mymemory" to "MyMemory", "libre" to "LibreTranslate"), s.translator) { v -> app.library.updateSettings { it.copy(translator = v) } }
        }
        if (s.translator == "libre") {
            var url by remember { mutableStateOf(s.libreUrl) }
            var key by remember { mutableStateOf(s.libreKey) }
            SearchBox(url, { v -> url = v; app.library.updateSettings { it.copy(libreUrl = v.trim()) } }, tr("Server address, such as https://translate.example.com"),
                onSubmit = {}, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), icon = WebIcons.Link, height = 36.dp)
            SearchBox(key, { v -> key = v; app.library.updateSettings { it.copy(libreKey = v.trim()) } }, tr("API key, if the server needs one"),
                onSubmit = {}, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), icon = null, height = 36.dp)
            Text(tr("LibreTranslate is open source. With your own server, caption lines go nowhere else."), style = Type.cardSub, color = look.text2,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

/** Settings › Storage: the cache and Clean cache, and in Pro mode each part with its size. */
@Composable
fun StorageSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    val ctx = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<CacheItem>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<CacheItem?>(null) }
    suspend fun measure() { items = withContext(Dispatchers.Default) { app.cacheItems(ctx) } }
    LaunchedEffect(Unit) { measure() }
    fun clean(ids: List<String>, done: String) {
        if (busy) return
        busy = true
        scope.launch {
            withContext(Dispatchers.Default) { ids.forEach { app.clearCache(it, ctx) } }
            measure(); busy = false; app.say(done)
        }
    }
    val list = items.orEmpty()
    val everyday = list.filter { it.everyday }
    SectionTitle(tr("Storage"), null)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(tr("Cache"), style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = look.text)
            Text(if (items == null) tr("Measuring…") else tr("${fmtBytes(everyday.sumOf { it.bytes })} of cover art, episode lists and other files that are downloaded again when needed."),
                style = Type.cardSub, color = look.text2, modifier = Modifier.padding(top = 2.dp))
        }
        Pill(tr(if (busy) "Cleaning…" else "Clean cache"), WebIcons.Trash, filled = false, onClick = { clean(everyday.map { it.id }, tr("Cache cleaned.")) })
    }
    if (s.pro && list.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(look.bg2)) {
            list.forEachIndexed { i, item ->
                if (i > 0) Separator(Modifier.padding(start = 16.dp))
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(tr(item.label), style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = look.text)
                            Text(fmtBytes(item.bytes) + if (item.files >= 0) " · " + tr("${item.files} files") else "", style = Type.cardSub, color = look.text3,
                                modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(tr(item.note), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(top = 2.dp))
                    }
                    TextButton(enabled = !busy && item.bytes > 0, onClick = { if (item.everyday) clean(listOf(item.id), tr("${tr(item.label)} cleaned.")) else confirm = item }) {
                        Text(tr("Clean"))
                    }
                }
            }
        }
        Text(tr("Saved captions and speech models aren’t part of Clean cache, because they take a while to make or download again."),
            style = Type.cardSub, color = look.text2, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
    }
    confirm?.let { item ->
        AlertDialog(onDismissRequest = { confirm = null },
            title = { Text(tr("Clean ${tr(item.label)}?")) },
            text = { Text(tr(item.note)) },
            confirmButton = { TextButton(onClick = { confirm = null; clean(listOf(item.id), tr("${tr(item.label)} cleaned.")) }) { Text(tr("Clean")) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr("Cancel")) } })
    }
}

private val LANG_NAMES = mapOf("en" to "English", "zh" to "中文", "zt" to "中文（繁體）", "ja" to "日本語", "ko" to "한국어", "es" to "Español", "fr" to "Français",
    "de" to "Deutsch", "it" to "Italiano", "pt" to "Português", "ru" to "Русский")
private fun packLabel(pack: String): String {
    val (a, b) = pack.removePrefix("translate-").split('_').let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
    return "${LANG_NAMES[a] ?: a} → ${LANG_NAMES[b] ?: b}"
}

/** Settings › Models: speech recognition models, translation language packs, and the video tool. */
@Composable
fun ModelSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val engine = app.platform.captions
    val packs = app.platform.packs
    SectionTitle(tr("Models"), tr("Speech recognition and translation, kept on this device."))
    if (engine != null && engine.models.isNotEmpty()) {
        var tick by remember { mutableStateOf(0) }
        var busy by remember { mutableStateOf<String?>(null) }
        var progress by remember { mutableStateOf(-1f) }
        Text(tr("Speech recognition (Whisper)"), style = Type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = look.text,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).background(look.bg2)) {
            engine.models.forEachIndexed { i, (name, size) ->
                val bytes = remember(tick, name) { engine.modelBytes(name) }
                if (i > 0) Separator(Modifier.padding(start = 16.dp))
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = Type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = look.text)
                            if (s.captionModel == name) Text(tr("In use"), style = Type.meta, color = look.accent, modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(if (bytes > 0) tr("Downloaded · ${fmtBytes(bytes)}") else tr("Not downloaded · $size"), style = Type.cardSub, color = look.text2)
                    }
                    when {
                        busy == name -> Text(if (progress >= 0f) "${(progress * 100).toInt()}%" else tr("Downloading…"), style = Type.cardSub, color = look.text2,
                            modifier = Modifier.padding(end = 12.dp))
                        bytes > 0 -> {
                            if (s.captionModel != name) TextButton(onClick = { app.library.updateSettings { it.copy(captionModel = name) } }) { Text(tr("Use")) }
                            TextButton(enabled = busy == null, onClick = { if (engine.delete(name)) { tick++; app.say(tr("Removed $name.")) } }) { Text(tr("Delete")) }
                        }
                        else -> TextButton(enabled = busy == null, onClick = {
                            busy = name; progress = -1f
                            scope.launch {
                                runCatching { engine.download(name) { _, p -> progress = p } }.onFailure { app.say(tr(it.message ?: "The speech model couldn’t be downloaded.")) }
                                busy = null; tick++
                            }
                        }) { Text(tr("Download")) }
                    }
                }
            }
        }
        Text(tr("Bigger models are more accurate and slower. A model is loaded only while captions are being made."), style = Type.cardSub, color = look.text2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    if (packs != null) {
        var installed by remember { mutableStateOf<List<String>?>(null) }
        var catalog by remember { mutableStateOf<List<String>>(emptyList()) }
        var working by remember { mutableStateOf<String?>(null) }
        suspend fun refresh() { installed = runCatching { packs.installed() }.getOrDefault(emptyList()) }
        LaunchedEffect(Unit) { if (packs.available) refresh() }
        Text(tr("Translation language packs"), style = Type.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = look.text,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 2.dp))
        Text(tr("Argos Translate packs, used by a LibreTranslate server on this computer for translation that never leaves it."), style = Type.cardSub, color = look.text2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        if (!packs.available) {
            Text(tr("To manage packs here, install Argos Translate: pip install argostranslate"), style = Type.cardSub, color = look.text3,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        } else {
            val have = installed.orEmpty()
            val more = catalog.filter { it !in have && it.removePrefix("translate-").split('_').all { c -> c in LANG_NAMES } }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(look.bg2)) {
                (have.map { it to true } + more.map { it to false }).forEachIndexed { i, (pack, isInstalled) ->
                    if (i > 0) Separator(Modifier.padding(start = 16.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(packLabel(pack), style = Type.body, color = if (isInstalled) look.text else look.text2, modifier = Modifier.weight(1f))
                        if (working == pack) Text(tr("Working…"), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(end = 12.dp))
                        else TextButton(enabled = working == null, onClick = {
                            working = pack
                            scope.launch { if (isInstalled) packs.remove(pack) else packs.install(pack); refresh(); working = null }
                        }) { Text(tr(if (isInstalled) "Remove" else "Download")) }
                    }
                }
                if (have.isEmpty() && more.isEmpty()) Text(tr(if (installed == null) "Measuring…" else "No language packs yet."), style = Type.cardSub, color = look.text2,
                    modifier = Modifier.padding(16.dp))
            }
            if (catalog.isEmpty()) TextButton(enabled = working == null, modifier = Modifier.padding(start = 8.dp), onClick = {
                working = "catalog"
                scope.launch { catalog = runCatching { packs.catalog() }.getOrDefault(emptyList()); working = null }
            }) { Text(tr(if (working == "catalog") "Looking…" else "Show packs to download")) }
        }
    }
    SectionTitle(tr("Video podcasts"), null)
    VideoTools(app, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
}

/** Settings › Updates: a quiet daily look for a new release on GitHub. */
@Composable
fun UpdateSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    val update by app.update.collectAsState()
    SectionTitle(tr("Updates"), null)
    SwitchRow(tr("Check for updates"), tr("Once a day, AfterhoursNex looks for a new release on GitHub. Nothing else is sent."), s.checkUpdates) { on ->
        app.library.updateSettings { it.copy(checkUpdates = on) }
    }
    if (AppInfo.REPO.isBlank() || s.pro) {
        var repo by remember { mutableStateOf(s.updateRepo.ifBlank { AppInfo.REPO }) }
        SearchBox(repo, { v -> repo = v; app.library.updateSettings { it.copy(updateRepo = v.trim()) } }, tr("GitHub repository, such as owner/Afterhours-Nex"),
            onSubmit = { app.checkUpdates(force = true) }, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), icon = WebIcons.Link, height = 36.dp)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Pill(tr("Check now"), WebIcons.Refresh, filled = false, onClick = { app.checkUpdates(force = true) })
        val note = update?.let { tr("AfterhoursNex ${it.version} is available") } ?: tr(app.updateNote)
        if (note.isNotBlank()) Text(note, style = Type.cardSub, color = if (update != null) look.accent else look.text2, modifier = Modifier.padding(start = 12.dp))
    }
    update?.let { r ->
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(tr("Download"), WebIcons.Export, filled = true, onClick = { app.platform.openUrl(r.download) })
            Pill(tr("What’s new"), null, filled = false, onClick = { app.platform.openUrl(r.page) })
        }
    }
}

/** Settings › Your computer (on a phone): borrow the computer's auto captions and video podcasts over Wi-Fi. */
@Composable
fun CompanionSettings(app: AppState) {
    if (app.platform.name != "Android") return
    val s by app.library.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf(s.companion) }
    var state by remember { mutableStateOf("") }
    fun connect() {
        val l = link.trim()
        if (!l.startsWith("http")) return
        state = tr("Connecting…")
        scope.launch {
            val before = Paired.link
            Paired.link = l
            state = try {
                val o = Paired.ping()
                app.library.updateSettings { it.copy(companion = l) }
                tr("Connected to AfterhoursNex ${o["version"]?.jsonPrimitive?.contentOrNull.orEmpty()} on your computer.")
            } catch (e: Exception) {
                Paired.link = before
                tr("Couldn’t reach your computer. Check that both are on the same Wi-Fi and that phone access is on.")
            }
        }
    }
    SectionTitle(tr("Your computer"), tr("Auto captions and video podcasts, made on your computer and played here."))
    Text(tr("On your computer, open AfterhoursNex › Settings › Use on your phone, turn on phone access, and paste its link here."),
        style = Type.cardSub, color = look.text2, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    SearchBox(link, { link = it }, tr("Phone link, such as http://192.168.1.5:8765/?key=…"), onSubmit = { connect() },
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), icon = WebIcons.Link, height = 36.dp)
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill(tr("Connect"), WebIcons.Check, filled = false, onClick = { connect() })
        if (s.companion.isNotBlank()) Pill(tr("Disconnect"), null, filled = false, onClick = {
            app.library.updateSettings { it.copy(companion = "") }; link = ""; state = ""
        })
    }
    if (state.isNotBlank()) Text(state, style = Type.cardSub, color = look.text2, modifier = Modifier.padding(horizontal = 16.dp))
}

/** The language's own name, for a code such as "ja". */
fun languageName(code: String): String = TRANSLATE_TO.firstOrNull { it.first == code }?.let { if (it.first == "auto") tr(it.second) else it.second } ?: code

/** Trying the translation service, and the translations kept for each language. */
@Composable
private fun TranslationManager(app: AppState) {
    val s by app.library.settings.collectAsState()
    val version by app.translator.version.collectAsState()
    val scope = rememberCoroutineScope()
    var sample by remember { mutableStateOf("") }
    val target = if (s.translateTo == "auto") I18n.lang else s.translateTo
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Pill(tr("Try it"), WebIcons.Globe, filled = false, onClick = {
            sample = tr("Translating…")
            scope.launch { sample = app.translator.sample("Good evening, and welcome to the show.", if (target.startsWith("en")) "ja" else target, s) ?: tr("The translation service didn’t answer.") }
        })
        if (sample.isNotBlank()) Text(sample, style = Type.cardSub, color = look.text2, modifier = Modifier.padding(start = 12.dp))
    }
    val counts = remember(version) { app.translator.counts().filterValues { it > 0 } }
    if (counts.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).glass(RoundedCornerShape(14.dp))) {
        counts.entries.forEachIndexed { i, (lang, n) ->
            if (i > 0) Separator(Modifier.padding(start = 16.dp))
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(languageName(lang), style = Type.body, color = look.text, modifier = Modifier.weight(1f))
                Text(tr("$n lines"), style = Type.cardSub, color = look.text2)
                TextButton(onClick = { app.translator.clear(lang) }) { Text(tr("Clear")) }
            }
        }
    }
}

/** The video tool: ready (update, remove), or one click to add it. On a phone, videos come from the paired computer. */
@Composable
fun VideoTools(app: AppState, modifier: Modifier = Modifier) {
    val bridge = Video.bridge
    val scope = rememberCoroutineScope()
    var tick by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var note by remember { mutableStateOf("") }
    val tool = remember(tick) { app.platform.videoTool }
    Column(modifier) {
        when {
            app.platform.name == "Android" -> Text(tr(if (Paired.ready) "Ready. Videos come from your computer." else "Video podcasts come from your computer: connect it in Settings › Your computer."),
                style = Type.cardSub, color = look.text2)
            bridge == null || !bridge.manageable -> {}
            tool != null -> {
                Text(tr("Ready. YouTube and Bilibili videos, channels and playlists play as audio, using yt-dlp at $tool."), style = Type.cardSub, color = look.text2)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(tr("Update"), WebIcons.Refresh, filled = false, onClick = { note = tr("Updating…"); scope.launch { note = bridge.update().ifBlank { tr("Up to date.") } } })
                    if (bridge.installedByApp) Pill(tr("Remove"), WebIcons.Trash, filled = false, onClick = { if (bridge.uninstall()) { tick++; note = "" } })
                }
                if (note.isNotBlank()) Text(note, style = Type.cardSub, color = look.text3, modifier = Modifier.padding(top = 6.dp))
            }
            else -> {
                Text(tr("Play YouTube and Bilibili videos, channels and playlists as audio. AfterhoursNex downloads its video tool, yt-dlp (about 35 MB), only if you want it."),
                    style = Type.cardSub, color = look.text2)
                val p = progress
                Pill(if (p != null) "${(p * 100).toInt()}%" else tr("Add video podcasts"), WebIcons.Export, filled = true, modifier = Modifier.padding(top = 10.dp), onClick = {
                    if (progress == null) scope.launch {
                        progress = 0f
                        val ok = runCatching { bridge.install { progress = it } }.getOrDefault(false)
                        progress = null; tick++
                        app.say(tr(if (ok) "Video podcasts are ready." else "Couldn’t download the video tool. Check your connection and try again."))
                    }
                })
            }
        }
    }
}
