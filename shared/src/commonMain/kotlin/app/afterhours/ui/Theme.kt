package app.afterhours.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFC8326C), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3), onPrimaryContainer = Color(0xFF3E0020),
    secondaryContainer = Color(0xFFF3E2E8), onSecondaryContainer = Color(0xFF2B1720),
    background = Color(0xFFFBF8F9), surface = Color(0xFFFBF8F9),
    surfaceVariant = Color(0xFFF1ECEE), onSurfaceVariant = Color(0xFF5B5559),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6E9F), onPrimary = Color(0xFF3E0020),
    primaryContainer = Color(0xFF5C1236), onPrimaryContainer = Color(0xFFFFD9E3),
    secondaryContainer = Color(0xFF3A2A31), onSecondaryContainer = Color(0xFFF3E2E8),
    background = Color(0xFF18181A), surface = Color(0xFF18181A),
    surfaceVariant = Color(0xFF2A292C), onSurfaceVariant = Color(0xFFB9B3B8),
)

@Composable
fun AfterhoursTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
