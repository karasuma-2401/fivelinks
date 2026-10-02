package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TeamBlue,
    onPrimary = Color.White,
    primaryContainer = TeamBlue.copy(alpha = 0.2f),
    onPrimaryContainer = TeamBlueLight,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = GoldAccent.copy(alpha = 0.2f),
    onSecondaryContainer = GoldGlow,
    tertiary = TeamGreen,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = Color(0xFFE2E8F0),
    error = TeamRed,
    onError = Color.White
)

@Composable
fun FiveLinksTheme(
    darkTheme: Boolean = true, // FiveLinks uses immersive dark casino board theme by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = FiveLinksTypography,
        content = content
    )
}
