package app.afterhours.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.afterhours.core.*

private const val ROW = 34

/** The sidebar's shows: pinned first, then the listener's groups, then the rest. Hold and drag to reorder; right-click for more. */
@Composable
fun ShowList(app: AppState, shows: List<Show>, current: Screen, modifier: Modifier) {
    var order by remember(shows) { mutableStateOf(sectionsOf(shows).map { (label, list) -> label to list.map { it.sid } }) }
    val bySid = remember(shows) { shows.associateBy { it.sid } }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val rowPx = with(LocalDensity.current) { ROW.dp.toPx() }
    LazyColumn(modifier) {
        order.forEachIndexed { si, (label, sids) ->
            if (label.isNotEmpty()) item(key = "h:$si:$label") {
                Text(label, style = Type.navHead, color = look.text3, modifier = Modifier.animateItem().padding(start = 10.dp, top = 12.dp, bottom = 4.dp))
            }
            items(sids, key = { it }) { sid ->
                val s = bySid[sid] ?: return@items
                val held = dragging == sid
                ShowRow(app, s, current, held, if (held) offset else 0f,
                    modifier = Modifier.animateItem(placementSpec = if (held) null else spring(dampingRatio = 0.82f, stiffness = 420f)),
                    onDragStart = { dragging = sid; offset = 0f },
                    onDrag = { dy ->
                        offset += dy
                        val list = order[si].second
                        val i = list.indexOf(sid)
                        val j = when { offset > rowPx / 2 && i < list.lastIndex -> i + 1; offset < -rowPx / 2 && i > 0 -> i - 1; else -> i }
                        if (j != i) {
                            val moved = list.toMutableList().apply { add(j, removeAt(i)) }
                            order = order.toMutableList().also { it[si] = label to moved }
                            offset -= (j - i) * rowPx                     // the row now sits in its new place; keep it under the pointer
                        }
                    },
                    onDragEnd = { dragging = null; offset = 0f; app.library.reorder(order.flatMap { it.second }) })
            }
        }
    }
}

private fun sectionsOf(shows: List<Show>): List<Pair<String, List<Show>>> {
    val pinned = shows.filter { it.pinned }
    val rest = shows.filterNot { it.pinned }
    val groups = rest.filter { it.group.isNotBlank() }.groupBy { it.group }
    val loose = rest.filter { it.group.isBlank() }
    return buildList {
        if (pinned.isNotEmpty()) add(tr("Pinned") to pinned)
        groups.forEach { (g, list) -> add(g to list) }
        if (loose.isNotEmpty()) add((if (pinned.isNotEmpty() || groups.isNotEmpty()) tr("Shows") else "") to loose)
    }
}

@Composable
private fun ShowRow(app: AppState, s: Show, current: Screen, held: Boolean, offset: Float, modifier: Modifier,
                    onDragStart: () -> Unit, onDrag: (Float) -> Unit, onDragEnd: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var menuAt by remember { mutableStateOf<androidx.compose.ui.unit.IntOffset?>(null) }
    val on = (current as? Screen.ShowPage)?.sid == s.sid
    val ia = remember { MutableInteractionSource() }
    val lift by animateFloatAsState(if (held) 1f else 0f, tween(220, easing = Motion.EaseOut), label = "lift")
    Box(modifier.fillMaxWidth().height(ROW.dp).zIndex(if (held) 2f else 0f)
        .graphicsLayer {
            translationY = offset
            scaleX = 1f + 0.035f * lift; scaleY = 1f + 0.035f * lift
            shadowElevation = 18f * lift; shape = RoundedCornerShape(8.dp); clip = false
        }
        .pointerInput(s.sid) {
            detectDragGesturesAfterLongPress(onDragStart = { onDragStart() }, onDragEnd = onDragEnd, onDragCancel = onDragEnd) { change, amount ->
                change.consume(); onDrag(amount.y)
            }
        }
        .pointerInput(s.sid) {
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent()
                    if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) {
                        val p = e.changes.first().position
                        menuAt = androidx.compose.ui.unit.IntOffset(p.x.toInt(), p.y.toInt()); menu = true
                        e.changes.forEach { it.consume() }
                    }
                }
            }
        }) {
        Row(Modifier.fillMaxSize().padding(vertical = 0.5.dp).clip(RoundedCornerShape(8.dp)).background(if (on || held) look.fill2 else Color.Transparent)
            .hoverFill(ia, RoundedCornerShape(8.dp), look.fill)
            .clickable(ia, null) { if (app.screen is Screen.ShowPage) app.stack.removeAt(app.stack.lastIndex); app.open(Screen.ShowPage(s.sid)) }
            .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ShowArt(s, Modifier.size(24.dp), corner = 6.dp, shadow = false)
            Spacer(Modifier.width(10.dp))
            Text(s.name, style = Type.nav.copy(fontWeight = FontWeight.Normal), color = if (s.status == "ready") look.text else look.text2,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (s.pinned) Icon(WebIcons.Pin, contentDescription = tr("Pinned"), tint = look.text3, modifier = Modifier.size(14.dp))
        }
        ShowMenu(app, s, menu, menuAt) { menu = false }
    }
}

/** Rename, pin, group or remove a show: the sidebar's right-click menu, and a long press on a Library tile. */
@Composable
fun ShowMenu(app: AppState, s: Show, expanded: Boolean, at: androidx.compose.ui.unit.IntOffset? = null, onDismiss: () -> Unit) {
    var dialog by remember { mutableStateOf("") }
    GlassMenu(expanded, onDismiss, at = at) {
        MenuItem(tr("Rename…"), { onDismiss(); dialog = "rename" })
        MenuItem(tr("Change cover…"), { onDismiss(); dialog = "cover" })
        MenuItem(tr(if (s.pinned) "Unpin" else "Pin to top"), { onDismiss(); app.library.setPinned(s.sid, !s.pinned) })
        MenuItem(tr("Move to group…"), { onDismiss(); dialog = "group" })
        MenuItem(tr("Remove…"), { onDismiss(); dialog = "remove" }, danger = true)
    }
    when (dialog) {
        "cover" -> CoverDialog(app, s) { dialog = "" }
        "rename" -> {
            var name by remember { mutableStateOf(s.name) }
            AlertDialog(onDismissRequest = { dialog = "" }, containerColor = look.pop, shape = RoundedCornerShape(22.dp), title = { Text(tr("Rename show")) },
                text = {
                    Column {
                        SearchBox(name, { name = it }, s.title, onSubmit = { app.library.rename(s.sid, name); dialog = "" }, modifier = Modifier.fillMaxWidth(), icon = null, height = 38.dp)
                        Text(tr("Leave it empty to use the show’s own name."), style = Type.cardSub, color = look.text2, modifier = Modifier.padding(top = 8.dp))
                    }
                },
                confirmButton = { TextButton(onClick = { app.library.rename(s.sid, name); dialog = "" }) { Text(tr("Rename")) } },
                dismissButton = { TextButton(onClick = { dialog = "" }) { Text(tr("Cancel")) } })
        }
        "group" -> {
            val groups = remember { app.library.shows.value.map { it.group }.filter { it.isNotBlank() }.distinct() }
            var g by remember { mutableStateOf(s.group) }
            AlertDialog(onDismissRequest = { dialog = "" }, containerColor = look.pop, shape = RoundedCornerShape(22.dp), title = { Text(tr("Move to group")) },
                text = {
                    Column {
                        SearchBox(g, { g = it }, tr("Group name, such as Commute"), onSubmit = { app.library.setGroup(s.sid, g); dialog = "" },
                            modifier = Modifier.fillMaxWidth(), icon = null, height = 38.dp)
                        if (groups.isNotEmpty()) LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(groups) { name -> Chip(name, g == name) { g = name } }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { app.library.setGroup(s.sid, g); dialog = "" }) { Text(tr("Move")) } },
                dismissButton = {
                    Row {
                        if (s.group.isNotBlank()) TextButton(onClick = { app.library.setGroup(s.sid, ""); dialog = "" }) { Text(tr("No group")) }
                        TextButton(onClick = { dialog = "" }) { Text(tr("Cancel")) }
                    }
                })
        }
        "remove" -> AlertDialog(onDismissRequest = { dialog = "" }, containerColor = look.pop, shape = RoundedCornerShape(22.dp), title = { Text(tr("Remove “${s.name}” from your library?")) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = ""
                    if ((app.screen as? Screen.ShowPage)?.sid == s.sid) app.back()
                    app.library.remove(s.sid)
                    app.sayUndo(tr("Removed ${s.name}.")) { app.library.undoRemove()?.let { app.say(tr("${it.name} is back.")) } }
                }) { Text(tr("Remove")) }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text(tr("Cancel")) } })
    }
}

/** Change a show's cover: upload an image, or design one with its own words, a gradient and a typeface. */
@Composable
private fun CoverDialog(app: AppState, s: Show, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val now = remember { Design.parse(s.coverStyle) }
    var words by remember { mutableStateOf(now?.text ?: s.name) }
    var gradient by remember { mutableStateOf(now?.gradient ?: 0) }
    var serif by remember { mutableStateOf(now?.serif ?: true) }
    val design = Design(gradient, serif, words).toString()
    AlertDialog(onDismissRequest = onDone, containerColor = look.pop, shape = RoundedCornerShape(22.dp), title = { Text(tr("Cover")) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Artwork("", words, Modifier.size(120.dp), corner = 14.dp, style = design)
                    Column(Modifier.padding(start = 16.dp)) {
                        SearchBox(words, { words = it }, tr("Words on the cover"), onSubmit = {}, modifier = Modifier.width(220.dp), icon = null, height = 36.dp)
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip(tr("Serif"), serif) { serif = true }
                            Chip(tr("Sans"), !serif) { serif = false }
                        }
                    }
                }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DESIGNS.forEachIndexed { i, colors ->
                        Box(Modifier.size(30.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(colors))
                            .border(if (i == gradient) 2.5.dp else 0.7.dp, if (i == gradient) look.text else look.sep, androidx.compose.foundation.shape.CircleShape)
                            .clickable { gradient = i })
                    }
                }
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(tr("Upload an image…"), WebIcons.Import, filled = false, onClick = {
                        scope.launch {
                            val f = app.platform.openFile(listOf("png", "jpg", "jpeg", "webp")) ?: return@launch
                            val ext = f.name.substringAfterLast('.', "png").lowercase()
                            val uri = app.platform.saveImage("cover-${s.sid.filter { it.isLetterOrDigit() }}-${Clock.now()}.$ext", f.bytes)
                            if (uri == null) app.say(tr("Couldn’t save that image.")) else { app.library.setCover(s.sid, uri); onDone() }
                        }
                    })
                    if (s.cover.isNotBlank() || s.coverStyle.isNotBlank()) Pill(tr("Use the show’s own cover"), null, filled = false, onClick = { app.library.resetCover(s.sid); onDone() })
                }
            }
        },
        confirmButton = { TextButton(onClick = { app.library.setCoverStyle(s.sid, design); onDone() }) { Text(tr("Use this design")) } },
        dismissButton = { TextButton(onClick = onDone) { Text(tr("Cancel")) } })
}
