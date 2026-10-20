package app.afterhours.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * The menu from the film: a rounded dark panel with a hairline rim and a deep soft shadow, items with rounded highlights,
 * and destructive ones in red. It grows in from where it opens: beside the pointer for a right-click ([at], in the
 * anchor's own coordinates), or under its button, and above it when there isn't room below.
 */
@Composable
fun GlassMenu(expanded: Boolean, onDismiss: () -> Unit, at: IntOffset? = null, width: Dp = 232.dp, content: @Composable ColumnScope.() -> Unit) {
    if (!expanded) return
    val provider = remember(at) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = (anchorBounds.left + (at?.x ?: 0)).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val below = if (at != null) anchorBounds.top + at.y else anchorBounds.bottom + 4
                val above = (if (at != null) anchorBounds.top + at.y else anchorBounds.top - 4) - popupContentSize.height
                val y = if (below + popupContentSize.height <= windowSize.height || above < 0) below else above
                return IntOffset(x, y.coerceAtLeast(0))
            }
        }
    }
    val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
    Popup(popupPositionProvider = provider, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(shown, enter = fadeIn(tween(140)) + scaleIn(tween(220, easing = Motion.EaseOut), initialScale = 0.92f, transformOrigin = TransformOrigin(0f, 0f))) {
            val shape = RoundedCornerShape(14.dp)
            val dark = look.dark
            Column(Modifier.padding(10.dp).width(width).heightIn(max = 460.dp)
                .shadow(26.dp, shape, ambientColor = Color(0x80000000), spotColor = Color(0x80000000)).clip(shape)
                .background(if (dark) Color(0xF2282830) else Color(0xF7FAFAFC))
                .border(1.dp, if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f), shape)
                .verticalScroll(rememberScrollState()).padding(6.dp), content = content)
        }
    }
}

/** An item in a [GlassMenu]: a rounded highlight under the pointer, a tick when it's the one in use, red when it removes something. */
@Composable
fun MenuItem(text: String, onClick: () -> Unit, checked: Boolean = false, danger: Boolean = false, note: String? = null) {
    val ia = remember { MutableInteractionSource() }
    val hovered by ia.collectIsHoveredAsState()
    val color = when { danger -> if (look.dark) Color(0xFFFF8A8A) else Color(0xFFD93D3D); checked -> look.accent; else -> look.text }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (hovered) look.text.copy(alpha = 0.08f) else Color.Transparent)
        .hoverable(ia).clickable(ia, null, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.body.copy(fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal), color = color, modifier = Modifier.weight(1f))
        if (note != null) Text(note, style = Type.cardSub, color = look.text2, modifier = Modifier.padding(start = 8.dp))
        if (checked) Icon(WebIcons.Check, contentDescription = null, tint = look.accent, modifier = Modifier.padding(start = 8.dp).size(16.dp))
    }
}

/** A small heading inside a [GlassMenu]. */
@Composable
fun MenuLabel(text: String) = Text(text, style = Type.meta, color = look.text3, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp))
