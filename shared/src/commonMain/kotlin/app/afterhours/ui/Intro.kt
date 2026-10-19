package app.afterhours.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.afterhours.core.AppInfo
import app.afterhours.core.tr
import app.afterhours.resources.Res
import app.afterhours.resources.afterhours_icon
import app.afterhours.resources.liberation_serif_italic
import app.afterhours.resources.liberation_serif_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlin.math.sqrt

// The sunset of the app icon, and its indigo sky.
private val Gold = Color(0xFFFEBB5F)
private val Apricot = Color(0xFFF7926D)
private val Coral = Color(0xFFF37377)
private val Rose = Color(0xFFCC5286)
private val Magenta = Color(0xFFB6448D)
private val Cream = Color(0xFFF5EEF4)
private val Lilac = Color(0xFFE6DAEE)
private val Sky = Brush.verticalGradient(listOf(Color(0xFF110C2B), Color(0xFF1B1444), Color(0xFF2A1C58)))
private val Smooth = CubicBezierEasing(0.2f, 0.75f, 0.25f, 1f)
private val Overshoot = CubicBezierEasing(0.3f, 1.6f, 0.45f, 1f)
private const val LENGTH = 3.9f

/**
 * The opening animation. The sunset icon rises, each letter of "Afterhours" rises in turn, "Nex" arrives in
 * sunset colours, and its x is drawn like two pen strokes with a sparkle where they cross. A tap skips it.
 */
@Composable
fun Intro(onDone: () -> Unit) {
    val serif = FontFamily(
        Font(Res.font.liberation_serif_regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.liberation_serif_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(LENGTH, tween(durationMillis = (LENGTH * 1000).toInt(), easing = LinearEasing))
        onDone()
    }
    val t = clock.value
    fun step(from: Float, length: Float) = Smooth.transform(((t - from) / length).coerceIn(0f, 1f))
    val icon = step(0.1f, 1.0f)
    val out = step(3.45f, 0.45f)
    Box(
        Modifier.fillMaxSize().graphicsLayer { alpha = 1f - out }.background(Sky)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {   // the afterglow below the horizon
            drawCircle(Brush.radialGradient(listOf(Coral.copy(alpha = 0.26f), Rose.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width / 2, size.height * 1.12f), radius = size.width * 0.9f), radius = size.width * 0.9f, center = Offset(size.width / 2, size.height * 1.12f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {   // halo and one ring of light around the icon
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(Brush.radialGradient(listOf(Apricot.copy(alpha = 0.34f * icon), Rose.copy(alpha = 0.1f * icon), Color.Transparent), center = c, radius = size.width / 2), radius = size.width / 2, center = c)
                    val ring = ((t - 0.9f) / 1.6f).coerceIn(0f, 1f)
                    if (ring > 0f && ring < 1f) {
                        val s = 112.dp.toPx() * (1f + 0.45f * ring)
                        drawRoundRect(Brush.linearGradient(listOf(Gold, Rose)), topLeft = Offset(c.x - s / 2, c.y - s / 2), size = Size(s, s),
                            cornerRadius = CornerRadius(s * 0.24f), alpha = 0.7f * (1f - ring), style = Stroke(width = 1.5.dp.toPx()))
                    }
                }
                Image(painterResource(Res.drawable.afterhours_icon), contentDescription = null, modifier = Modifier.size(112.dp).graphicsLayer {
                    alpha = icon
                    translationY = (1f - icon) * 26.dp.toPx()
                    scaleX = 0.92f + 0.08f * icon
                    scaleY = 0.92f + 0.08f * icon
                })
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
                "Afterhours".forEachIndexed { i, ch -> Letter(ch, serif, FontStyle.Normal, null, step(0.6f + i * 0.055f, 0.75f)) }
                Letter('N', serif, FontStyle.Italic, Brush.linearGradient(listOf(Gold, Apricot)), step(1.2f, 0.75f))
                Letter('e', serif, FontStyle.Italic, Brush.linearGradient(listOf(Apricot, Coral)), step(1.29f, 0.75f))
                DrawnX(serif, t)
            }
            val rule = step(2.35f, 0.8f)
            Canvas(Modifier.padding(top = 10.dp).width(250.dp).height(10.dp)) {   // the ornamental rule
                val c = Offset(size.width / 2, size.height / 2)
                val reach = (size.width / 2 - 8.dp.toPx()) * rule
                val line = 1.dp.toPx()
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Apricot), startX = c.x - reach, endX = c.x), topLeft = Offset(c.x - 6.dp.toPx() - reach, c.y - line / 2), size = Size(reach, line))
                drawRect(Brush.horizontalGradient(listOf(Apricot, Color.Transparent), startX = c.x, endX = c.x + reach), topLeft = Offset(c.x + 6.dp.toPx(), c.y - line / 2), size = Size(reach, line))
                rotate(45f, pivot = c) {
                    val d = 6.dp.toPx() * Overshoot.transform(rule)
                    drawRect(Brush.linearGradient(listOf(Gold, Rose)), topLeft = Offset(c.x - d / 2, c.y - d / 2), size = Size(d, d))
                }
            }
            val words = step(2.6f, 0.9f)
            Text(tr(AppInfo.SLOGAN), fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, color = Lilac, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp).widthIn(max = 360.dp).graphicsLayer { alpha = words; translationY = (1f - words) * 10.dp.toPx() })
        }
    }
}

@Composable
private fun Letter(ch: Char, serif: FontFamily, style: FontStyle, brush: Brush?, p: Float) {
    Text(ch.toString(), fontFamily = serif, fontStyle = style, fontSize = 48.sp, color = Cream,
        style = if (brush != null) TextStyle(brush = brush) else TextStyle.Default,
        modifier = Modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 18.dp.toPx() })
}

/** The x of "Nex", drawn as two pen strokes (the bold one first), with a sparkle where they cross. */
@Composable
private fun DrawnX(serif: FontFamily, t: Float) {
    val bold = Smooth.transform(((t - 1.62f) / 0.36f).coerceIn(0f, 1f))
    val fine = Smooth.transform(((t - 1.95f) / 0.3f).coerceIn(0f, 1f))
    val settle = ((t - 2.2f) / 0.7f).coerceIn(0f, 1f)
    val spark = ((t - 2.22f) / 0.95f).coerceIn(0f, 1f)
    Box {
        Text("x", fontFamily = serif, fontStyle = FontStyle.Italic, fontSize = 48.sp, style = TextStyle(brush = Brush.linearGradient(listOf(Coral, Magenta))),
            modifier = Modifier
                .graphicsLayer { rotationZ = -9f * (1f - Overshoot.transform(settle)) }
                .drawWithContent {
                    val w = size.width
                    val h = size.height
                    val reveal = Path()
                    reveal.addBand(Offset(0f, h * 0.3f), Offset(w, h * 0.95f), bold, h * 0.45f)
                    reveal.addBand(Offset(w, h * 0.3f), Offset(0f, h * 0.95f), fine, h * 0.45f)
                    clipPath(reveal) { this@drawWithContent.drawContent() }
                })
        if (spark > 0f && spark < 1f) {
            Canvas(Modifier.matchParentSize()) {
                val c = Offset(size.width * 0.5f, size.height * 0.63f)
                val grow = if (spark < 0.35f) spark / 0.35f else 1f - (spark - 0.35f) / 0.65f * 0.5f
                val alpha = if (spark < 0.35f) spark / 0.35f else 1f - (spark - 0.35f) / 0.65f
                val r = size.minDimension * 0.42f * grow
                drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.5f * alpha), Color.Transparent), center = c, radius = r * 2.4f + 1f), radius = r * 2.4f + 1f, center = c)
                rotate(90f * spark, pivot = c) {
                    val star = Path().apply {
                        moveTo(c.x, c.y - r)
                        quadraticTo(c.x, c.y, c.x + r, c.y)
                        quadraticTo(c.x, c.y, c.x, c.y + r)
                        quadraticTo(c.x, c.y, c.x - r, c.y)
                        quadraticTo(c.x, c.y, c.x, c.y - r)
                        close()
                    }
                    drawPath(star, Color(0xFFFFF6E8).copy(alpha = alpha))
                }
            }
        }
    }
}

/** A band along one stroke of the x, grown from its start to [progress] of the way along. */
private fun Path.addBand(from: Offset, to: Offset, progress: Float, thickness: Float) {
    if (progress <= 0f) return
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length == 0f) return
    val nx = -dy / length * thickness / 2
    val ny = dx / length * thickness / 2
    val bx = from.x - dx / length * thickness * 0.3f
    val by = from.y - dy / length * thickness * 0.3f
    val ex = from.x + dx * progress
    val ey = from.y + dy * progress
    moveTo(bx + nx, by + ny)
    lineTo(ex + nx, ey + ny)
    lineTo(ex - nx, ey - ny)
    lineTo(bx - nx, by - ny)
    close()
}
