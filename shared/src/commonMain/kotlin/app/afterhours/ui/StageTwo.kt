package app.afterhours.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.afterhours.core.*

/** Settings › Language. */
@Composable
fun LanguageSettings(app: AppState) {
    val s by app.library.settings.collectAsState()
    SectionTitle(tr("Language"), null)
    Text(tr("Podcast titles and descriptions stay as their publishers wrote them."), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState())) {
        Choice(listOf("system" to tr("System"), "en" to "English", "zh-Hans" to "简体中文", "ja" to "日本語"), s.language) { v ->
            app.library.updateSettings { it.copy(language = v) }
        }
    }
}

/** Settings › Use on your phone (desktop only). */
@Composable
fun PhoneSettings(app: AppState) {
    val link = app.platform.phone ?: return
    val st by link.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    SectionTitle(tr("Use on your phone"), null)
    SwitchRow(tr("Phone access"), tr("Let phones on the same Wi-Fi use AfterhoursNex from this computer, with a private link."), st.on) { on -> link.setEnabled(on) }
    if (st.error.isNotBlank()) Text(tr(st.error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
    if (st.on && st.url.isNotBlank()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
            st.qr?.let { QrCode(it, Modifier.size(168.dp)) }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(tr("Scan the code, or open this link on your phone, then add it to your home screen. The phone keeps its own shelf and progress."), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer {
                    Text(st.url, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 10.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp))
                }
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(st.url)); app.say(tr("Link copied.")) }) { Text(tr("Copy link")) }
                    TextButton(onClick = { link.renew(); app.say(tr("The old link no longer works. Use the new one on your phones.")) }) { Text(tr("New link")) }
                }
                Text(tr("Keep this computer awake while you listen. If a phone can’t connect, let AfterhoursNex through your firewall on private networks."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QrCode(matrix: List<BooleanArray>, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(8.dp)) {
        val n = matrix.size
        if (n == 0) return@Canvas
        val cell = size.minDimension / n
        for (y in 0 until n) for (x in 0 until n) if (matrix[y][x]) drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell + .5f, cell + .5f))
    }
}
