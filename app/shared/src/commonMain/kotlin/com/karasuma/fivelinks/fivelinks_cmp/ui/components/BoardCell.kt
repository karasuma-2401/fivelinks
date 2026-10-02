package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BoardCellBg
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BoardCellBorder
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.HighlightAttack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.HighlightValid

@Composable
fun BoardCell(
    position: BoardPosition,
    card: Card?,
    chipTeam: Team?,
    isCorner: Boolean,
    isLockedSequence: Boolean,
    isValidPlacement: Boolean,
    isValidSnipeTarget: Boolean,
    isLastMove: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val shape = RoundedCornerShape(3.dp)

    val borderColor = when {
        isValidPlacement -> HighlightValid.copy(alpha = pulseAlpha)
        isValidSnipeTarget -> HighlightAttack.copy(alpha = pulseAlpha)
        else -> BoardCellBorder.copy(alpha = 0.8f)
    }

    val borderWidth = when {
        isValidPlacement || isValidSnipeTarget -> 1.75.dp
        else -> 0.5.dp
    }

    val bgModifier = when {
        isValidPlacement -> Modifier.background(HighlightValid.copy(alpha = pulseAlpha * 0.2f))
        isValidSnipeTarget -> Modifier.background(HighlightAttack.copy(alpha = pulseAlpha * 0.2f))
        else -> Modifier.background(BoardCellBg)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(0.75.dp)
            .clip(shape)
            .then(bgModifier)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCorner) {
            CornerCellView()
        } else if (card != null) {
            BoardCellCardView(card = card)
        }

        // Frosted Chip on top of the card (underlying card remains visible)
        if (chipTeam != null) {
            ChipView(
                team = chipTeam,
                isLocked = isLockedSequence,
                isLastMove = isLastMove,
                modifier = Modifier.fillMaxSize(0.88f)
            )
        }
    }
}
