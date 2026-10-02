package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.ui.graphics.Color
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team

// Background & Surfaces (Modern Matte Espresso Dark)
val BackgroundDark = Color(0xFF141312)        // Warm matte espresso black table
val SurfaceDark = Color(0xFF1D1B19)           // Elevated dark surface
val SurfaceElevated = Color(0xFF2A2825)       // Subtle card/modal surface

// Board & Cells (Classic Ivory / Linen Board from reference image)
val BoardContainerBg = Color(0xFFF5EFEB)      // Warm cream/ivory linen container
val BoardCellBg = Color(0xFFFAF6F0)           // Light linen cell background
val BoardCellBorder = Color(0xFFE5DDD3)       // Soft cell divider line

// Compatibility aliases
val BoardBackground = BoardContainerBg
val CellBackground = BoardCellBg
val CellBorder = BoardCellBorder

// Header & Typography Colors
val TextPrimary = Color(0xFFF7F5F2)           // Off-white headline
val TextSecondary = Color(0xFF8E8B85)         // Muted gray for DECK / TURN
val TurnBadgeBg = Color(0xFF5A3816)           // Warm caramel/amber badge background
val TurnBadgeText = Color(0xFFF5A623)         // Golden amber badge text

// Team & Acrylic Frosted Chip Colors (matching reference image)
val TeamBlue = Color(0xFF4A72D8)              // Crisp vibrant periwinkle blue
val TeamBlueChip = Color(0x994A72D8)          // Translucent frosted blue (~60% alpha)
val TeamBlueBorder = Color(0xFF4A72D8)
val TeamBlueGlow = Color(0xFF7B9DF6)
val TeamBlueLight = Color(0xFF93C5FD)

val TeamRed = Color(0xFFEB5378)               // Crisp vibrant coral-rose red
val TeamRedChip = Color(0x99EB5378)           // Translucent frosted rose (~60% alpha)
val TeamRedBorder = Color(0xFFEB5378)
val TeamRedGlow = Color(0xFFF87171)
val TeamRedLight = Color(0xFFFCA5A5)

val TeamGreen = Color(0xFF2E9A68)             // Crisp emerald green
val TeamGreenChip = Color(0x992E9A68)
val TeamGreenBorder = Color(0xFF2E9A68)
val TeamGreenGlow = Color(0xFF34D399)
val TeamGreenLight = Color(0xFF6EE7B7)

// Tactical Highlights & Accents
val GoldAccent = Color(0xFFEAA036)            // Warm gold / corner star
val GoldGlow = Color(0xFFFCD34D)              // Aura
val HighlightValid = Color(0xFF2E9A68)        // Subtle legal move green
val HighlightAttack = Color(0xFFEB5378)       // Subtle snipe attack red
val HighlightSelected = Color(0xFFEAA036)     // Gold highlight for selection

// Physical Card Face Colors (Crisp printing on white card stock)
val CardSurfaceWhite = Color(0xFFFFFFFF)      // Pure card stock white
val CardSurfaceLight = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFD6CFC4)            // Soft tactile card edge
val CardSuitRed = Color(0xFFD32F2F)           // Pure crimson red
val CardSuitBlack = Color(0xFF1E1E1E)         // Pure deep charcoal black
val CornerGold = Color(0xFFEAA036)            // Star gold

fun Team.primaryColor(): Color = when (this) {
    Team.BLUE -> TeamBlue
    Team.RED -> TeamRed
    Team.GREEN -> TeamGreen
}

fun Team.chipFillColor(): Color = when (this) {
    Team.BLUE -> TeamBlueChip
    Team.RED -> TeamRedChip
    Team.GREEN -> TeamGreenChip
}

fun Team.chipBorderColor(): Color = when (this) {
    Team.BLUE -> TeamBlueBorder
    Team.RED -> TeamRedBorder
    Team.GREEN -> TeamGreenBorder
}

fun Team.glowColor(): Color = when (this) {
    Team.BLUE -> TeamBlueGlow
    Team.RED -> TeamRedGlow
    Team.GREEN -> TeamGreenGlow
}

fun Team.lightColor(): Color = when (this) {
    Team.BLUE -> TeamBlueLight
    Team.RED -> TeamRedLight
    Team.GREEN -> TeamGreenLight
}
