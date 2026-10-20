package app.afterhours.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** The colours of the Afterhours web app, light and dark. */
@Immutable
data class Look(
    val bg: Color, val bg2: Color, val side: Color, val bar: Color, val fill: Color, val fill2: Color,
    val text: Color, val text2: Color, val text3: Color, val sep: Color,
    val accent: Color, val accentInk: Color, val accentSoft: Color, val on: Color, val pop: Color, val dark: Boolean,
)

val LightLook = Look(
    bg = Color(0xFFFFFFFF), bg2 = Color(0xFFF5F5F7), side = Color(0xFFF2F2F5), bar = Color(0xD1FAFAFC),
    fill = Color(0x1A767680), fill2 = Color(0x2E767680), text = Color(0xFF1D1D1F), text2 = Color(0xFF5E5E64), text3 = Color(0xFF737379),
    sep = Color(0x1F3C3C43), accent = Color(0xFFC8326C), accentInk = Color.White, accentSoft = Color(0x1AC8326C), on = Color(0xFF34C759),
    pop = Color.White, dark = false,
)

val DarkLook = Look(
    bg = Color(0xFF111114), bg2 = Color(0xFF1B1B1F), side = Color(0xFF17171B), bar = Color(0xC21E1E23),
    fill = Color(0x2E9A9AA6), fill2 = Color(0x479A9AA6), text = Color(0xFFF4F4F7), text2 = Color(0xFFBDBDC5), text3 = Color(0xFF9A9AA3),
    sep = Color(0x1CFFFFFF), accent = Color(0xFFFF82AE), accentInk = Color(0xFF2B0715), accentSoft = Color(0x2EFF82AE), on = Color(0xFF34D058),
    pop = Color(0xFF232328), dark = true,
)

val LocalLook = staticCompositionLocalOf { LightLook }
/** The left and right margin of a page: 4% of its width, between 20 and 48 points, as on the web. */
val LocalGutter = staticCompositionLocalOf { 20.dp }
/** True on phone-sized windows. */
val LocalCompact = staticCompositionLocalOf { true }
/** Room left at the bottom of a page for the floating player. */
val LocalBottomSpace = staticCompositionLocalOf { 0.dp }
/** How see-through the sidebar, player and tab bar are: 0 solid, 1 the most glass-like (Settings › Appearance). */
val LocalGlass = staticCompositionLocalOf { 0.2f }

val look: Look @Composable @ReadOnlyComposable get() = LocalLook.current
val gutter: Dp @Composable @ReadOnlyComposable get() = LocalGutter.current

/** The web app's type sizes. */
object Type {
    val pageTitle = TextStyle(fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.022).em)
    val pageSub = TextStyle(fontSize = 17.sp, lineHeight = 23.sp)
    val sectionTitle = TextStyle(fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.015).em)
    val sectionSub = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
    val epTitle = TextStyle(fontSize = 16.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
    val epDesc = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
    val meta = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    val pill = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp)
    val cardTitle = TextStyle(fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
    val cardSub = TextStyle(fontSize = 13.sp, lineHeight = 17.sp)
    val nav = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val navHead = TextStyle(fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold)
    val brand = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.015).em)
    val tab = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
}

private fun scheme(l: Look) = if (l.dark) darkColorScheme(
    primary = l.accent, onPrimary = l.accentInk, primaryContainer = l.accentSoft, onPrimaryContainer = l.text,
    secondaryContainer = l.fill2, onSecondaryContainer = l.text, background = l.bg, onBackground = l.text, surface = l.bg, onSurface = l.text,
    surfaceVariant = l.bg2, onSurfaceVariant = l.text2, outline = l.sep, outlineVariant = l.sep,
    surfaceContainer = l.pop, surfaceContainerHigh = l.pop, surfaceContainerHighest = l.pop, surfaceContainerLow = l.bg2,
) else lightColorScheme(
    primary = l.accent, onPrimary = l.accentInk, primaryContainer = l.accentSoft, onPrimaryContainer = l.text,
    secondaryContainer = l.fill2, onSecondaryContainer = l.text, background = l.bg, onBackground = l.text, surface = l.bg, onSurface = l.text,
    surfaceVariant = l.bg2, onSurfaceVariant = l.text2, outline = l.sep, outlineVariant = l.sep,
    surfaceContainer = l.pop, surfaceContainerHigh = l.pop, surfaceContainerHighest = l.pop, surfaceContainerLow = l.bg2,
)

private val Typo = Typography(
    headlineMedium = Type.pageTitle, titleLarge = Type.sectionTitle, titleMedium = Type.epTitle, titleSmall = Type.cardTitle,
    bodyLarge = Type.body, bodyMedium = Type.epDesc, bodySmall = Type.cardSub, labelLarge = Type.pill, labelMedium = Type.meta,
)

@Composable
fun AfterhoursTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    LookProvider(if (dark) DarkLook else LightLook, content)
}

/** Applies a set of colours to everything inside, Material components included. */
@Composable
fun LookProvider(l: Look, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLook provides l) {
        MaterialTheme(colorScheme = scheme(l), typography = Typo) {
            CompositionLocalProvider(LocalContentColor provides l.text, content = content)
        }
    }
}
