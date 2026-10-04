package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import fivelinks_cmp.app.shared.generated.resources.Res
import fivelinks_cmp.app.shared.generated.resources.montserrat
import org.jetbrains.compose.resources.Font

/**
 * Montserrat is bundled as a variable font (wght axis) so every weight renders
 * the same on every device. Relying on FontFamily.SansSerif let OEM font
 * pickers (e.g. Samsung's handwriting fonts) take over the whole UI.
 */
@Composable
fun montserratFontFamily(): FontFamily {
    val light = Font(Res.font.montserrat, FontWeight.Light)
    val regular = Font(Res.font.montserrat, FontWeight.Normal)
    val medium = Font(Res.font.montserrat, FontWeight.Medium)
    val semiBold = Font(Res.font.montserrat, FontWeight.SemiBold)
    val bold = Font(Res.font.montserrat, FontWeight.Bold)
    val extraBold = Font(Res.font.montserrat, FontWeight.ExtraBold)
    val black = Font(Res.font.montserrat, FontWeight.Black)
    return remember(light, regular, medium, semiBold, bold, extraBold, black) {
        FontFamily(light, regular, medium, semiBold, bold, extraBold, black)
    }
}

@Composable
fun fiveLinksTypography(): Typography {
    val family = montserratFontFamily()
    return remember(family) {
        fun style(weight: FontWeight, size: Int, lineHeight: TextUnit, letterSpacing: Double) = TextStyle(
            fontFamily = family,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing.sp
        )
        Typography(
            displayLarge = style(FontWeight.Black, 48, 56.sp, 0.0),
            displayMedium = style(FontWeight.Black, 40, 48.sp, 0.0),
            displaySmall = style(FontWeight.Black, 34, 40.sp, 0.0),
            headlineLarge = style(FontWeight.Black, 28, 34.sp, 0.5),
            headlineMedium = style(FontWeight.ExtraBold, 22, 28.sp, 0.5),
            headlineSmall = style(FontWeight.ExtraBold, 20, 26.sp, 0.25),
            titleLarge = style(FontWeight.Bold, 18, 24.sp, 0.25),
            titleMedium = style(FontWeight.SemiBold, 15, 20.sp, 0.15),
            titleSmall = style(FontWeight.SemiBold, 13, 18.sp, 0.1),
            // Default style for bare Text(): leave the line height to the font so
            // one-off font sizes never get squeezed into a fixed 22sp line.
            bodyLarge = style(FontWeight.Normal, 15, TextUnit.Unspecified, 0.15),
            bodyMedium = style(FontWeight.Normal, 13, 19.sp, 0.15),
            bodySmall = style(FontWeight.Normal, 11, 16.sp, 0.2),
            labelLarge = style(FontWeight.Bold, 13, 16.sp, 1.0),
            labelMedium = style(FontWeight.Bold, 11, 14.sp, 1.0),
            labelSmall = style(FontWeight.SemiBold, 9, 12.sp, 0.5)
        )
    }
}
