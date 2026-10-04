package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme

@Composable
fun BoardCell(
    card: Card?,
    chipTeam: Team?,
    isCorner: Boolean,
    isValidPlacement: Boolean,
    isValidSnipeTarget: Boolean,
    isDimmed: Boolean,
    isLastMove: Boolean,
    cellSize: Dp,
    pulse: State<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FiveLinksTheme.colors
    val background = if (isCorner || card == null) colors.cornerCell else card.suit.cardBackground()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(0.8.dp)
            .clip(RoundedCornerShape(cellSize * 0.12f))
            .background(background)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .drawWithContent {
                drawContent()
                when {
                    isValidPlacement -> drawPlacementGlow(pulse.value)
                    isValidSnipeTarget -> drawCrosshair(pulse.value)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            isCorner -> CornerStarVector(modifier = Modifier.size(cellSize * 0.46f))
            card != null -> BoardCellCardView(card = card, cellSize = cellSize)
        }

        // Dim the card but not the chip, so the position stays readable.
        if (isDimmed) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .background(colors.cellScrim)
            )
        }

        if (chipTeam != null) {
            ChipView(
                team = chipTeam,
                isLastMove = isLastMove,
                pulse = pulse,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun DrawScope.drawPlacementGlow(pulse: Float) {
    drawRect(Color.White.copy(alpha = 0.08f + 0.14f * pulse))
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = Color.White.copy(alpha = 0.55f + 0.45f * pulse),
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(size.minDimension * 0.12f),
        style = Stroke(width = stroke)
    )
}

/**
 * Lock-on brackets in the cell corners around an opponent chip that can be
 * sniped; they breathe with the pulse. Corners stay clear of the chip's white rim.
 */
private fun DrawScope.drawCrosshair(pulse: Float) {
    val color = Color.White.copy(alpha = 0.7f + 0.3f * pulse)
    val stroke = 2.2.dp.toPx()
    val inset = stroke / 2 + size.minDimension * (0.03f + 0.06f * (1f - pulse))
    val arm = size.minDimension * 0.24f
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    listOf(
        Offset(left, top) to Offset(1f, 1f),
        Offset(right, top) to Offset(-1f, 1f),
        Offset(left, bottom) to Offset(1f, -1f),
        Offset(right, bottom) to Offset(-1f, -1f)
    ).forEach { (corner, dir) ->
        drawLine(color, corner, Offset(corner.x + dir.x * arm, corner.y), stroke, StrokeCap.Round)
        drawLine(color, corner, Offset(corner.x, corner.y + dir.y * arm), stroke, StrokeCap.Round)
    }
}
