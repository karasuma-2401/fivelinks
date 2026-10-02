package com.karasuma.fivelinks.fivelinks_cmp.ui.theme

import androidx.compose.ui.graphics.Color
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team

// Background & Surface
val BackgroundDark = Color(0xFF0F172A)     // Slate 900
val SurfaceDark = Color(0xFF1E293B)        // Slate 800
val SurfaceElevated = Color(0xFF334155)    // Slate 700
val BoardBackground = Color(0xFF0B132B)    // Deep Navy Felt
val CellBackground = Color(0xFF1C2541)     // Dark Card Cell
val CellBorder = Color(0xFF3A506B)         // Cell Border subtle

// Team Colors
val TeamBlue = Color(0xFF2563EB)           // Royal Blue
val TeamBlueGlow = Color(0xFF60A5FA)
val TeamBlueLight = Color(0xFF93C5FD)

val TeamRed = Color(0xFFDC2626)            // Crimson Red
val TeamRedGlow = Color(0xFFF87171)
val TeamRedLight = Color(0xFFFCA5A5)

val TeamGreen = Color(0xFF059669)          // Emerald Green
val TeamGreenGlow = Color(0xFF34D399)
val TeamGreenLight = Color(0xFF6EE7B7)

// Tactical Highlights & Accents
val GoldAccent = Color(0xFFF59E0B)         // Amber 500
val GoldGlow = Color(0xFFFCD34D)           // Divine Golden Aura
val HighlightValid = Color(0xFF10B981)     // Green pulse for legal moves
val HighlightAttack = Color(0xFFEF4444)    // Red crosshair for snipe removes
val HighlightSelected = Color(0xFF8B5CF6)  // Purple for hand card selection

// Card Colors
val CardSurfaceLight = Color(0xFFF8FAFC)
val CardBorder = Color(0xFFCBD5E1)
val CardSuitRed = Color(0xFFE11D48)        // Rose Red
val CardSuitBlack = Color(0xFF1E293B)      // Deep Slate Black
val CornerGold = Color(0xFFF59E0B)

fun Team.primaryColor(): Color = when (this) {
    Team.BLUE -> TeamBlue
    Team.RED -> TeamRed
    Team.GREEN -> TeamGreen
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
