package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = PureWhite,
    primaryContainer = BrandRed.copy(alpha = 0.12f),
    onPrimaryContainer = BrandRedDeep,
    secondary = BrandDark,
    onSecondary = PureWhite,
    tertiary = TeamGreen,
    onTertiary = PureWhite,
    background = BackgroundLight,
    onBackground = BrandDark,
    surface = SurfaceLight,
    onSurface = BrandDark,
    surfaceVariant = SurfaceLightElevated,
    onSurfaceVariant = TextMutedLight,
    surfaceTint = Color.Transparent,
    inverseSurface = BrandDark,
    inverseOnSurface = PureWhite,
    outline = BorderLight,
    outlineVariant = Color(0xFFE9E5E0),
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Color(0xFFFBFAF8),
    surfaceContainer = Color(0xFFF4F2EF),
    surfaceContainerHigh = SurfaceLightElevated,
    surfaceContainerHighest = Color(0xFFE9E6E2),
    error = BrandRed,
    onError = PureWhite
)

private val NightColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = PureWhite,
    primaryContainer = BrandRed.copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFFFB3B8),
    secondary = TextOnNight,
    onSecondary = SurfaceNight,
    tertiary = TeamGreen,
    onTertiary = PureWhite,
    background = BackgroundNight,
    onBackground = TextOnNight,
    surface = SurfaceNight,
    onSurface = TextOnNight,
    surfaceVariant = SurfaceNightElevated,
    onSurfaceVariant = TextMutedNight,
    surfaceTint = Color.Transparent,
    inverseSurface = TextOnNight,
    inverseOnSurface = SurfaceNight,
    outline = BorderNight,
    outlineVariant = Color(0xFF2F2C29),
    surfaceContainerLowest = Color(0xFF0F0E0D),
    surfaceContainerLow = Color(0xFF1A1816),
    surfaceContainer = SurfaceNight,
    surfaceContainerHigh = Color(0xFF24221F),
    surfaceContainerHighest = SurfaceNightElevated,
    error = BrandRed,
    onError = PureWhite
)

/** Game-specific colours that have no Material 3 slot. */
@Immutable
data class FiveLinksColors(
    val isNight: Boolean,
    val boardFrame: Color,
    val cellScrim: Color,
    val cornerCell: Color,
    val cardEdge: Color
)

private val LightGameColors = FiveLinksColors(
    isNight = false,
    boardFrame = PureWhite,
    cellScrim = BackgroundLight.copy(alpha = 0.62f),
    cornerCell = Color(0xFFF3EFE9),
    cardEdge = Color.Transparent
)

private val NightGameColors = FiveLinksColors(
    isNight = true,
    boardFrame = SurfaceNight,
    cellScrim = BackgroundNight.copy(alpha = 0.58f),
    cornerCell = SurfaceNightElevated,
    cardEdge = PureWhite.copy(alpha = 0.1f)
)

private val LocalFiveLinksColors = staticCompositionLocalOf { LightGameColors }

object FiveLinksTheme {
    val colors: FiveLinksColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFiveLinksColors.current
}

@Composable
fun FiveLinksTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalFiveLinksColors provides if (darkTheme) NightGameColors else LightGameColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) NightColorScheme else LightColorScheme,
            typography = fiveLinksTypography(),
            content = content
        )
    }
}
