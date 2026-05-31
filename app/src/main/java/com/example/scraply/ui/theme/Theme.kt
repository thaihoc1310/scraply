package com.example.scraply.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = PaperCream,
    primaryContainer = AccentLight,
    onPrimaryContainer = InkBlack,
    secondary = AccentLight,
    onSecondary = InkBlack,
    tertiary = InkSoft,
    onTertiary = PaperCream,
    background = PaperCream,
    onBackground = InkBlack,
    surface = PaperCream,
    onSurface = InkBlack,
    surfaceVariant = PaperCreamDark,
    onSurfaceVariant = InkSoft,
    outline = InkMuted,
    error = WarmRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = AccentLight,
    onPrimary = InkBlack,
    primaryContainer = Accent,
    onPrimaryContainer = DarkOnSurface,
    secondary = Accent,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2A2623),
    onSurfaceVariant = DarkOnSurface,
    outline = InkMuted,
    tertiary = AccentLight,
    onTertiary = InkBlack,
    error = WarmRed,
    onError = Color.White,
)

@Composable
fun ScraplyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
