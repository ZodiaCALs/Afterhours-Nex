package app.afterhours.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

/** The web app's motion curves (its --ease-* variables). */
object Motion {
    /** Fluent's decelerate: fast start, long gentle settle. Pages, panels and anything arriving. */
    val EaseOut = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f)
    val EaseMid = CubicBezierEasing(0f, 0f, 0f, 1f)
    /** Fluent's accelerate, for things leaving. */
    val EaseIn = CubicBezierEasing(0.9f, 0.1f, 1f, 0.2f)
    /** A plain ease for colour changes. */
    val EaseStd = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)
}

/** Shrinks slightly while pressed, as everything clickable does on the web (scale .97). */
@Composable
fun Modifier.pressable(interaction: MutableInteractionSource, scaleTo: Float = 0.97f): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scaleTo else 1f, tween(if (pressed) 120 else 320, easing = Motion.EaseOut), label = "press")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** A soft highlight under the pointer (desktop and Android with a mouse). */
@Composable
fun Modifier.hoverFill(interaction: MutableInteractionSource, shape: Shape, color: Color): Modifier {
    val hovered by interaction.collectIsHoveredAsState()
    val c by animateColorAsState(if (hovered) color else color.copy(alpha = 0f), tween(180, easing = Motion.EaseStd), label = "hover")
    return this.hoverable(interaction).background(c, shape)
}
