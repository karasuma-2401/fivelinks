package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipSequence
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

@Composable
fun BoardGrid(
    gameState: GameState,
    validPlacementPositions: Set<BoardPosition>,
    validSnipePositions: Set<BoardPosition>,
    onCellClick: (BoardPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastMovePos: BoardPosition? = when (val lm = gameState.lastMove) {
        is Move.Place -> lm.position
        is Move.CraftPlace -> lm.position
        is Move.Remove -> lm.position
        is Move.CraftRemove -> lm.position
        else -> null
    }
    // While a card is selected everything that is not a legal target is dimmed.
    val hasTargets = validPlacementPositions.isNotEmpty() || validSnipePositions.isNotEmpty()

    // One shared pulse for every highlight; cells only read it while drawing.
    val pulse = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val colors = FiveLinksTheme.colors
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boardDim = when {
            maxWidth > 10.dp && maxHeight > 10.dp -> minOf(maxWidth, maxHeight)
            maxWidth > 10.dp -> maxWidth
            else -> 320.dp
        }
        val framePadding = 5.dp
        val cellSize = (boardDim - framePadding * 2) / 10
        val frameShape = RoundedCornerShape(18.dp)

        Box(
            modifier = Modifier
                .size(boardDim)
                .shadow(if (colors.isNight) 0.dp else 16.dp, frameShape)
                .clip(frameShape)
                .background(colors.boardFrame)
                .then(
                    if (colors.isNight) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, frameShape)
                    } else {
                        Modifier
                    }
                )
                .padding(framePadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 10) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (c in 0 until 10) {
                            // Shared instances keep the click lambdas stable across recompositions.
                            val pos = BoardPosition.all[r * 10 + c]
                            val isCorner = gameState.board.isCorner(pos)
                            val isValidPlacement = pos in validPlacementPositions
                            val isValidSnipe = pos in validSnipePositions

                            BoardCell(
                                card = if (isCorner) null else gameState.board.cardAt(pos),
                                chipTeam = gameState.chips.at(pos),
                                isCorner = isCorner,
                                isValidPlacement = isValidPlacement,
                                isValidSnipeTarget = isValidSnipe,
                                isDimmed = hasTargets && !isValidPlacement && !isValidSnipe,
                                isLastMove = pos == lastMovePos,
                                cellSize = cellSize,
                                pulse = pulse,
                                onClick = {
                                    if (isValidPlacement || isValidSnipe) {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    }
                                    onCellClick(pos)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SequenceLinks(
                sequences = gameState.completedSequence,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Draws a bar through the five chips of every completed sequence: the "five links". */
@Composable
private fun SequenceLinks(
    sequences: List<ChipSequence>,
    modifier: Modifier = Modifier
) {
    sequences.forEach { sequence ->
        key(sequence) {
            SequenceLink(sequence = sequence, modifier = modifier)
        }
    }
}

@Composable
private fun SequenceLink(
    sequence: ChipSequence,
    modifier: Modifier = Modifier
) {
    val ends = remember(sequence) {
        val ordered = sequence.positions.sortedWith(compareBy({ it.row }, { it.column }))
        ordered.first() to ordered.last()
    }
    // The bar is drawn from one end to the other when the sequence completes.
    val reveal = remember(sequence) { Animatable(0f) }
    LaunchedEffect(sequence) {
        reveal.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val cell = size.width / 10f
        fun centerOf(pos: BoardPosition) = Offset((pos.column + 0.5f) * cell, (pos.row + 0.5f) * cell)

        val start = centerOf(ends.first)
        val end = start + (centerOf(ends.second) - start) * reveal.value
        val drop = Offset(0f, cell * 0.06f)
        drawLine(Color.Black.copy(alpha = 0.28f), start + drop, end + drop, cell * 0.24f, StrokeCap.Round)
        drawLine(Color.White, start, end, cell * 0.2f, StrokeCap.Round)
        drawLine(sequence.team.primaryColor(), start, end, cell * 0.1f, StrokeCap.Round)
    }
}
