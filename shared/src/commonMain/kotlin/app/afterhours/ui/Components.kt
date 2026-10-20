package app.afterhours.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.afterhours.core.*
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch

private val ArtShadow = Color(0x731C123A)

/** A show's colour, from its title: the hue of its monogram and of its page's header. */
fun hueOf(title: String): Float = ((title.hashCode() and 0x7fffffff) % 360).toFloat()

/** Cover art, with a soft shadow and a gradient monogram while it loads or when there's none. */
@Composable
fun Artwork(url: String, title: String, modifier: Modifier = Modifier, corner: Dp = 10.dp, shadow: Boolean = true) {
    val hue = remember(title) { hueOf(title) }
    val letter = remember(title) { title.trim().take(1).uppercase() }
    val measurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(corner)
    val base = if (shadow) modifier.shadow(10.dp, shape, clip = false, ambientColor = ArtShadow, spotColor = ArtShadow) else modifier
    Box(base.clip(shape).drawWithCache {
        val brush = Brush.linearGradient(listOf(Color.hsl(hue, 0.55f, 0.62f), Color.hsl((hue + 40f) % 360f, 0.5f, 0.42f)), Offset.Zero, Offset(size.width, size.height))
        val layout = if (size.minDimension >= 8f && letter.isNotEmpty())
            measurer.measure(letter, TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.minDimension * 0.46f).toSp())) else null
        onDrawBehind {
            drawRect(brush)
            if (layout != null) drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2f, (size.height - layout.size.height) / 2f))
        }
    }) {
        if (url.isNotBlank()) AsyncImage(model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(260).build(),
            contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun Separator(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(0.6.dp).background(look.sep))

/** A page's big title, its line underneath, and anything that sits to its right. */
@Composable
fun PageTitle(title: String, sub: String?, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = if (LocalCompact.current) 22.dp else 34.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.pageTitle, color = look.text)
            if (sub != null) Text(sub, style = Type.pageSub, color = look.text2, modifier = Modifier.padding(top = 6.dp))
        }
        if (trailing != null) Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = trailing)
    }
}

/** A section: the hairline above it, its title and line, and its tools on the right. */
@Composable
fun SectionTitle(title: String, sub: String?, tools: (@Composable RowScope.() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = 30.dp, bottom = 14.dp)) {
        Separator()
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(title, style = Type.sectionTitle, color = look.text)
                if (sub != null) Text(sub, style = Type.sectionSub, color = look.text2, modifier = Modifier.padding(top = 4.dp))
            }
            if (tools != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = tools)
        }
    }
}

/** The web app's play button: a soft accent pill, filled while its episode plays. */
@Composable
fun Pill(text: String, icon: ImageVector?, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, big: Boolean = false) {
    val ia = remember { MutableInteractionSource() }
    val fg by animateColorAsState(if (filled) look.accentInk else look.accent, tween(260, easing = Motion.EaseStd), label = "pill")
    val bg by animateColorAsState(if (filled) look.accent else look.accentSoft, tween(260, easing = Motion.EaseStd), label = "pill")
    Row(modifier.pressable(ia).height(if (big) 36.dp else 30.dp).clip(CircleShape).background(bg).clickable(ia, null, onClick = onClick)
        .padding(start = if (big) 14.dp else 11.dp, end = if (big) 18.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (big) 18.dp else 15.dp))
        if (icon != null) Spacer(Modifier.width(5.dp))
        Text(text, style = if (big) Type.pill.copy(fontSize = 14.sp) else Type.pill, color = fg, maxLines = 1)
    }
}

/** A round grey button with an icon, like the show page's refresh and remove buttons. */
@Composable
fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 36.dp,
                bg: Color = look.fill, tint: Color = look.text, enabled: Boolean = true) {
    val ia = remember { MutableInteractionSource() }
    Box(modifier.pressable(ia, 0.92f).size(size).clip(CircleShape).background(bg).hoverFill(ia, CircleShape, look.fill)
        .clickable(ia, null, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(size * 0.5f))
    }
}

/** A filter chip, black when selected. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val ia = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) look.text else look.fill, tween(240, easing = Motion.EaseStd), label = "chip")
    val fg by animateColorAsState(if (selected) look.bg else look.text, tween(240, easing = Motion.EaseStd), label = "chip")
    Box(Modifier.pressable(ia).height(32.dp).clip(CircleShape).background(bg).clickable(ia, null, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center) {
        Text(text, style = Type.pill.copy(fontSize = 13.5.sp), color = fg, maxLines = 1)
    }
}

/** The rounded grey field from the sidebar: "Search or paste a link". */
@Composable
fun SearchBox(value: String, onValueChange: (String) -> Unit, placeholder: String, onSubmit: () -> Unit, modifier: Modifier = Modifier,
              icon: ImageVector? = WebIcons.Search, height: Dp = 32.dp) {
    BasicTextField(value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = Type.body.copy(color = look.text), cursorBrush = SolidColor(look.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        modifier = modifier.onPreviewKeyEvent { if (it.key == Key.Enter && it.type == KeyEventType.KeyUp) { onSubmit(); true } else false },
        decorationBox = { field ->
            Row(Modifier.height(height).clip(RoundedCornerShape(9.dp)).background(look.fill).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, contentDescription = null, tint = look.text3, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(7.dp)) }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = Type.body, color = look.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    field()
                }
            }
        })
}

/** The green switch the web app uses. */
@Composable
fun Toggle(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled,
        colors = SwitchDefaults.colors(checkedTrackColor = look.on, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = look.fill2, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
}

/** The circular arrow with "15" or "30" inside, for skipping back and forward. */
@Composable
fun SkipIcon(back: Boolean, seconds: Int, tint: Color, size: Dp = 26.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(if (back) WebIcons.Back else WebIcons.Fwd, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize())
        Text("$seconds", color = tint, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.3f).sp, modifier = Modifier.padding(top = (size.value * 0.08f).dp))
    }
}

/** A row of choices, one of them selected (Settings). */
@Composable
fun Choice(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(9.dp)).background(look.fill).padding(2.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Text(label, style = Type.pill, color = if (on) look.text else look.text2,
                modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(if (on) (if (look.dark) look.fill2 else Color.White) else Color.Transparent)
                    .clickable { onSelect(value) }.padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

/**
 * An episode, as the web app lists it: the show and date in small type, the title, two lines of description,
 * and a play pill. On Listen Now (with [showTitle]) the cover sits on the left.
 */
@Composable
fun EpisodeRow(app: AppState, ep: Episode, showTitle: String? = null, list: List<Episode>? = null, modifier: Modifier = Modifier) {
    val progressMap = app.library.progress.collectAsState()
    val p by remember(ep.id) { derivedStateOf { progressMap.value[ep.id] } }
    val currentId by app.currentId.collectAsState()
    val playingNow by app.playingNow.collectAsState()
    val isCurrent = currentId == ep.id
    val playing = isCurrent && playingNow
    var expanded by remember(ep.id) { mutableStateOf(false) }
    val compact = LocalCompact.current
    val ia = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (isCurrent) look.fill else look.fill.copy(alpha = 0f), tween(320, easing = Motion.EaseStd), label = "row")
    Column(modifier.fillMaxWidth().padding(horizontal = gutter - 12.dp)) {
        Row(Modifier.fillMaxWidth().pressable(ia, 0.99f).clip(shape).background(bg).hoverFill(ia, shape, look.fill.copy(alpha = 0.55f))
            .clickable(ia, null) { expanded = !expanded }.animateContentSize(tween(320, easing = Motion.EaseOut)).padding(12.dp), verticalAlignment = Alignment.Top) {
            if (showTitle != null) {
                Artwork(ep.image.ifBlank { app.library.show(ep.sid)?.image.orEmpty() }, showTitle, Modifier.size(70.dp), corner = 8.dp)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showTitle != null) {
                        Text(showTitle, style = Type.meta, color = look.accent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(fmtDate(ep.date), style = Type.meta, color = look.text3, maxLines = 1)
                }
                Text(ep.title, style = Type.epTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                if (ep.desc.isNotBlank()) {
                    Text(ep.desc, style = Type.epDesc, color = look.text2, maxLines = if (expanded) 40 else 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp).widthIn(max = if (compact) 9999.dp else 680.dp))
                }
                val pr = p
                val status = when {
                    playing -> tr("Playing")
                    pr?.done == true -> tr("Played")
                    pr != null && pr.t > 10 && pr.d > pr.t -> tr(fmtLength(pr.d - pr.t) + " left")
                    else -> fmtLength(ep.duration).ifBlank { tr("Play") }
                }
                Pill(status, if (playing) WebIcons.Wave else WebIcons.Play, filled = playing,
                    onClick = { if (isCurrent) app.playback.toggle() else app.playback.play(ep, list ?: app.library.sorted(ep.sid)) },
                    modifier = Modifier.padding(top = 10.dp))
            }
        }
        if (!isCurrent) Separator(Modifier.padding(horizontal = 12.dp))
    }
}

/** A show in a grid or a row: its cover, title and author, as on the web Library page. */
@Composable
fun ShowTile(show: Show, onClick: () -> Unit) {
    val ia = remember { MutableInteractionSource() }
    Column(Modifier.pressable(ia).clip(RoundedCornerShape(12.dp)).hoverFill(ia, RoundedCornerShape(12.dp), look.fill.copy(alpha = 0.5f))
        .clickable(ia, null, onClick = onClick).padding(6.dp)) {
        Artwork(show.image, show.title, Modifier.fillMaxWidth().aspectRatio(1f), corner = 10.dp)
        Text(show.title, style = Type.cardTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
        val sub = when (show.status) { "pending" -> tr("Finding feed…"); "missing" -> tr("Not found automatically"); else -> show.author }
        if (sub.isNotBlank()) Text(sub, style = Type.cardSub, color = look.text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

/** A horizontal row of cards with "See all" and arrow buttons, as on Listen Now and Discover. */
@Composable
fun CardRow(title: String, sub: String?, onSeeAll: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val compact = LocalCompact.current
    SectionTitle(title, sub) {
        if (onSeeAll != null) Text(tr("See all"), style = Type.body, color = look.accent, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSeeAll).padding(4.dp))
        if (!compact) {
            RoundButton(WebIcons.Chevleft, tr("Scroll left"), { scope.launch { state.animateScrollBy(-640f) } }, size = 30.dp)
            RoundButton(WebIcons.Chevright, tr("Scroll right"), { scope.launch { state.animateScrollBy(640f) } }, size = 30.dp)
        }
    }
    LazyRow(state = state, contentPadding = PaddingValues(horizontal = gutter - 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 10.dp).clip(RoundedCornerShape(16.dp)).background(look.bg2).padding(18.dp), content = content)
}
