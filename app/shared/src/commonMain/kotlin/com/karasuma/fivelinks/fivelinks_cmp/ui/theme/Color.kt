package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.ui.graphics.Color
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team

// Brand (reference style: coral red + charcoal printed on warm white)
val BrandRed = Color(0xFFFB4853)
val BrandRedDeep = Color(0xFFD93441)
val BrandDark = Color(0xFF293130)
val PureWhite = Color(0xFFFFFFFF)

// Light theme
val BackgroundLight = Color(0xFFF7F5F2)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceLightElevated = Color(0xFFEFECE8)
val BorderLight = Color(0xFFDDD9D3)
val TextMutedLight = Color(0xFF7A7775)

// Night theme ("turn your lights down low")
val BackgroundNight = Color(0xFF141312)
val SurfaceNight = Color(0xFF1D1B19)
val SurfaceNightElevated = Color(0xFF2A2825)
val BorderNight = Color(0xFF3A3733)
val TextOnNight = Color(0xFFF2F0ED)
val TextMutedNight = Color(0xFF9C978F)

// Playing cards: red suits on coral, black suits on charcoal, white print
val CardBgRed = BrandRed
val CardBgBlack = BrandDark
val CardContentWhite = PureWhite

// Corner star & legendary (Divine Wipe) accent
val GoldAccent = Color(0xFFEAA036)

// Teams
val TeamBlue = Color(0xFF2563EB)
val TeamRed = Color(0xFFE5323F)
val TeamGreen = Color(0xFF10B981)

fun Team.primaryColor(): Color = when (this) {
    Team.BLUE -> TeamBlue
    Team.RED -> TeamRed
    Team.GREEN -> TeamGreen
}

/** Top-left highlight of the glossy chip body. */
fun Team.chipHighlight(): Color = when (this) {
    Team.BLUE -> Color(0xFF7EA6FF)
    Team.RED -> Color(0xFFFF8F97)
    Team.GREEN -> Color(0xFF6EE7B7)
}

/** Bottom-right shade of the glossy chip body. */
fun Team.chipShade(): Color = when (this) {
    Team.BLUE -> Color(0xFF173E9C)
    Team.RED -> Color(0xFF9E1525)
    Team.GREEN -> Color(0xFF0A7353)
}
