package app.afterhours.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Real Gaussian blur behind floating surfaces, as Apple's vibrancy, Microsoft's Acrylic and Google's blurred surfaces do:
 * the page draws itself into a layer once per frame, and each surface over it draws that layer, blurred, behind itself.
 * No library needed. Where blur isn't available (Android before 12), the surfaces are simply translucent.
 */
class Backdrop {
    internal var layer: GraphicsLayer? = null
    internal var origin = Offset.Zero
    internal var frame by mutableStateOf(0)        // goes up whenever the page redraws, so the surfaces redraw with it
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** The page behind the floating surfaces. */
@Composable
fun Modifier.backdropSource(backdrop: Backdrop): Modifier {
    val layer = rememberGraphicsLayer()
    SideEffect { backdrop.layer = layer }
    return this.onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            Snapshot.withoutReadObservation { backdrop.frame++ }
        }
}

/** A surface of frosted glass: the page behind it, Gaussian-blurred, under a tint. */
@Composable
fun Modifier.frosted(shape: Shape, tint: Color, radius: Dp = 30.dp): Modifier {
    val backdrop = LocalBackdrop.current ?: return this.clip(shape).background(tint.copy(alpha = maxOf(tint.alpha, 0.94f)), shape)
    val blurred = rememberGraphicsLayer()
    val px = with(LocalDensity.current) { radius.toPx() }
    var here by remember { mutableStateOf(Offset.Zero) }
    return this.onGloballyPositioned { here = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            backdrop.frame                              // redraw whenever the page does
            val source = backdrop.layer
            if (source != null && size.width >= 1f && size.height >= 1f) {
                blurred.renderEffect = BlurEffect(px, px, TileMode.Clamp)
                blurred.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
                    translate(backdrop.origin.x - here.x, backdrop.origin.y - here.y) { drawLayer(source) }
                }
                drawLayer(blurred)
            }
            drawRect(tint)
        }
}
