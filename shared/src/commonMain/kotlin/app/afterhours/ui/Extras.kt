package app.afterhours.ui

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

private val TRANSLATE_TO = listOf("auto" to "Interface language", "en" to "English", "zh-Hans" to "简体中文", "ja" to "日本語", "ko" to "한국어",
    "es" to "Español", "fr" to "Français", "de" to "Deutsch")

/** Settings › Caption translation. */
@Composable
fun TranslateSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    SectionTitle(tr("Caption translation"), null)
    SwitchRow(tr("Translate captions"), tr("Shows each caption line in your language too. The lines are sent to MyMemory, a free translation service; nothing else leaves your device."),
        s.translate) { on -> app.library.updateSettings { it.copy(translate = on) } }
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
