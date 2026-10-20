package app.afterhours.ui

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
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
fun Artwork(url: String, title: String, modifier: Modifier = Modifier, corner: Dp = 10.dp, shadow: Boolean = true, small: Boolean = false, style: String = "") {
    val hue = remember(title) { hueOf(title) }
    val letter = remember(title) { title.trim().take(1).uppercase() }
    val design = remember(style) { Design.parse(style) }
    val measurer = rememberTextMeasurer()
    val shape = RoundedCornerShape(corner)
    val base = if (shadow) modifier.shadow(10.dp, shape, clip = false, ambientColor = ArtShadow, spotColor = ArtShadow) else modifier
    Box(base.clip(shape).drawWithCache {
        val colors = design?.let { DESIGNS[it.gradient.mod(DESIGNS.size)] } ?: listOf(Color.hsl(hue, 0.55f, 0.62f), Color.hsl((hue + 40f) % 360f, 0.5f, 0.42f))
        val brush = Brush.linearGradient(colors, Offset.Zero, Offset(size.width, size.height))
        val words = design?.text?.trim()?.ifBlank { null }
        val layout = when {
            size.minDimension < 8f -> null
            design != null && words != null -> measurer.measure(words,
                TextStyle(color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    fontFamily = if (design.serif) FontFamily.Serif else FontFamily.SansSerif,
                    fontSize = (size.minDimension * if (words.length <= 3) 0.4f else 0.17f).toSp(),
                    lineHeight = (size.minDimension * if (words.length <= 3) 0.44f else 0.2f).toSp()),
                maxLines = 4, constraints = Constraints(maxWidth = (size.width * 0.84f).toInt().coerceAtLeast(1)))
            letter.isNotEmpty() -> measurer.measure(letter, TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.minDimension * 0.46f).toSp()))
            else -> null
        }
        onDrawBehind {
            drawRect(brush)
            if (layout != null) drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2f, (size.height - layout.size.height) / 2f))
        }
    }) {
        if (url.isNotBlank() && design == null) AsyncImage(model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(260).apply { if (small) size(160) }.build(),
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
    val sheen = Color.White.copy(alpha = if (filled) 0.24f else if (look.dark) 0.06f else 0.35f)
    Row(modifier.pressable(ia).height(if (big) 38.dp else 32.dp)
        .then(if (filled) Modifier.shadow(6.dp, CircleShape, ambientColor = look.accent.copy(alpha = 0.35f), spotColor = look.accent.copy(alpha = 0.35f)) else Modifier)
        .clip(CircleShape).background(bg).background(Brush.verticalGradient(0f to sheen, 0.55f to Color.Transparent))
        .border(0.7.dp, if (filled) Color.White.copy(alpha = 0.18f) else look.accent.copy(alpha = 0.16f), CircleShape).clickable(ia, null, onClick = onClick)
        .padding(start = if (big) 14.dp else 11.dp, end = if (big) 18.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (big) 18.dp else 15.dp))
        if (icon != null) Spacer(Modifier.width(5.dp))
        Text(text, style = if (big) Type.pill.copy(fontSize = 14.sp) else Type.pill, color = fg, maxLines = 1)
    }
}

/** A round grey button with an icon, like the show page's refresh and remove buttons. */
@Composable
fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 36.dp,
                bg: Color = Color.Unspecified, tint: Color = look.text, enabled: Boolean = true) {
    val ia = remember { MutableInteractionSource() }
    Box(modifier.pressable(ia, 0.92f).size(size).then(if (bg == Color.Unspecified) Modifier.glass(CircleShape) else Modifier.clip(CircleShape).background(bg))
        .hoverFill(ia, CircleShape, look.fill)
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
    Box(Modifier.pressable(ia).height(32.dp).then(if (selected) Modifier.clip(CircleShape).background(bg) else Modifier.glass(CircleShape))
        .clickable(ia, null, onClick = onClick).padding(horizontal = 14.dp),
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
            Row(Modifier.height(height).glass(RoundedCornerShape(10.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, contentDescription = null, tint = look.text3, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(7.dp)) }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = Type.body, color = look.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    field()
                }
            }
        })
}

/**
 * A switch made of liquid glass: a translucent track with a bright rim and sheen, and a glossy knob that stretches as
 * it slides and swells while it's pressed.
 */
@Composable
fun Toggle(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val ia = remember { MutableInteractionSource() }
    val pressed by ia.collectIsPressedAsState()
    val t by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 380f), label = "toggle")
    val squeeze by animateFloatAsState(if (pressed) 1f else 0f, tween(180, easing = Motion.EaseOut), label = "toggle")
    val onColor = look.on
    val offColor = look.fill2
    val dark = look.dark
    Box(modifier.size(51.dp, 31.dp).alpha(if (enabled) 1f else 0.4f)
        .toggleable(value = checked, interactionSource = ia, indication = null, enabled = enabled && onCheckedChange != null, role = Role.Switch) { onCheckedChange?.invoke(it) }
        .drawBehind {
            val r = CornerRadius(size.height / 2)
            // the track: tinted glass, a sheen on its upper half, and a fine bright rim
            drawRoundRect(lerp(offColor, onColor, t), cornerRadius = r)
            drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.22f else 0.38f), Color.Transparent), endY = size.height * 0.6f), cornerRadius = r)
            drawRoundRect(Color.White.copy(alpha = if (dark) 0.18f else 0.45f), cornerRadius = r, style = Stroke(width = 0.8.dp.toPx()))
            // the knob: stretches mid-slide, swells while pressed
            val pad = 2.dp.toPx()
            val h = size.height - pad * 2
            val w = h + 7.dp.toPx() * kotlin.math.sin(kotlin.math.PI.toFloat() * t) + 6.dp.toPx() * squeeze
            val x = pad + (size.width - pad * 2 - w) * t
            val knob = CornerRadius(h / 2)
            drawRoundRect(Color.Black.copy(alpha = 0.10f), Offset(x, pad + 1.5.dp.toPx()), Size(w, h), knob)
            drawRoundRect(Color.Black.copy(alpha = 0.06f), Offset(x - 0.5.dp.toPx(), pad + 2.5.dp.toPx()), Size(w + 1.dp.toPx(), h), knob)
            drawRoundRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFEDEDF2)), startY = pad, endY = pad + h), Offset(x, pad), Size(w, h), knob)
            drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(x + w * 0.22f, pad + h * 0.12f), Size(w * 0.4f, h * 0.22f), CornerRadius(h * 0.11f))
            drawRoundRect(lerp(offColor, onColor, t).copy(alpha = 0.22f), Offset(x, pad), Size(w, h), knob, style = Stroke(width = 1.dp.toPx()))
        })
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
        Row(Modifier.fillMaxWidth().pressable(ia, 0.99f).then(if (isCurrent) Modifier.glass(shape, strong = true) else Modifier.clip(shape).background(bg))
            .hoverFill(ia, shape, look.fill.copy(alpha = 0.55f))
            .clickable(ia, null) { expanded = !expanded }.animateContentSize(tween(320, easing = Motion.EaseOut)).padding(12.dp), verticalAlignment = Alignment.Top) {
            if (showTitle != null) {
                val sh = app.library.show(ep.sid)
                Artwork(ep.image.ifBlank { sh?.art.orEmpty() }, showTitle, Modifier.size(70.dp), corner = 8.dp, style = if (ep.image.isBlank()) sh?.coverStyle.orEmpty() else "")
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showTitle != null) {
                        Text(showTitle, style = Type.meta, color = look.accent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(9.dp))
                    }
                    if (ep.date > 0) Text(fmtDate(ep.date), style = Type.meta, color = look.text3, maxLines = 1)
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ShowTile(show: Show, onMenu: (() -> Unit)? = null, onClick: () -> Unit) {
    val ia = remember { MutableInteractionSource() }
    Column(Modifier.pressable(ia).clip(RoundedCornerShape(12.dp)).hoverFill(ia, RoundedCornerShape(12.dp), look.fill.copy(alpha = 0.5f))
        .combinedClickable(interactionSource = ia, indication = null, onLongClick = onMenu, onClick = onClick)
        .then(if (onMenu == null) Modifier else Modifier.pointerInput(show.sid) {
            awaitPointerEventScope { while (true) { val e = awaitPointerEvent(); if (e.type == PointerEventType.Press && e.buttons.isSecondaryPressed) onMenu() } }
        }).padding(6.dp)) {
        ShowArt(show, Modifier.fillMaxWidth().aspectRatio(1f), corner = 10.dp)
        Text(show.name, style = Type.cardTitle, color = look.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
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
    Column(modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 10.dp).glass(RoundedCornerShape(18.dp), strong = true).padding(18.dp), content = content)
}

/** A show's cover: the listener's own (an image or a design) if they chose one, else the show's, with its name for the monogram. */
@Composable
fun ShowArt(show: Show, modifier: Modifier = Modifier, corner: Dp = 10.dp, shadow: Boolean = true) =
    Artwork(show.art, show.name, modifier, corner, shadow, style = show.coverStyle)

/** The gradients a designed cover can use. */
val DESIGNS = listOf(
    listOf(Color(0xFFFF82AE), Color(0xFFB6448D)), listOf(Color(0xFFFEBB5F), Color(0xFFF37377)), listOf(Color(0xFF8E7CFF), Color(0xFF4B3FD1)),
    listOf(Color(0xFF4FD1C5), Color(0xFF2C7A9B)), listOf(Color(0xFFF7926D), Color(0xFF6A3FD1)), listOf(Color(0xFF9AE6B4), Color(0xFF2F855A)),
    listOf(Color(0xFFE2E8F0), Color(0xFF718096)), listOf(Color(0xFF3B3650), Color(0xFF0E0E12)))

/** A designed cover: a gradient, a typeface and the words on it ("g=<gradient>;f=<serif|sans>;t=<text>"). */
class Design(val gradient: Int, val serif: Boolean, val text: String) {
    override fun toString() = "g=$gradient;f=${if (serif) "serif" else "sans"};t=$text"
    companion object {
        fun parse(s: String): Design? {
            if (!s.startsWith("g=")) return null
            val parts = s.split(';', limit = 3)
            return Design(parts.getOrNull(0)?.removePrefix("g=")?.toIntOrNull() ?: 0, parts.getOrNull(1) != "f=sans", parts.getOrNull(2)?.removePrefix("t=").orEmpty())
        }
    }
}

/**
 * The glass every control and surface shares, after Apple's Liquid Glass: a translucent fill, a soft sheen across the top
 * and a hairline rim, so buttons, fields, chips, rows and cards all read as one material.
 */
@Composable
fun Modifier.glass(shape: Shape, strong: Boolean = false): Modifier {
    val l = look
    val fill = if (strong) l.fill2.copy(alpha = l.fill2.alpha * 0.8f) else l.fill
    val sheen = Color.White.copy(alpha = if (l.dark) 0.07f else 0.42f)
    val rim = if (l.dark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.06f)
    return this.clip(shape).background(fill, shape).background(Brush.verticalGradient(0f to sheen, 0.5f to Color.Transparent), shape).border(0.7.dp, rim, shape)
}

/** A message at the bottom, in glass, with its action (such as Undo) as a pill. */
@Composable
fun GlassSnackbar(data: androidx.compose.material3.SnackbarData) {
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier.padding(16.dp).widthIn(max = 560.dp).shadow(20.dp, shape, ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
        .clip(shape).background(look.pop.copy(alpha = 0.94f)).glass(shape).padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(data.visuals.message, style = Type.body, color = look.text, modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp))
        data.visuals.actionLabel?.let { label -> Pill(label, null, filled = true, onClick = { data.performAction() }) }
    }
}
