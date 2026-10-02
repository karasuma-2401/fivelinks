package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BoardContainerBg

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

    val lockedPositions = gameState.completedSequence.flatMap { it.positions }.toSet()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(14.dp)
        val boardDim = if (maxHeight in 1.dp..maxWidth) maxHeight else maxWidth

        Box(
            modifier = Modifier
                .size(boardDim)
                .padding(4.dp)
                .shadow(12.dp, shape)
                .clip(shape)
                .background(BoardContainerBg)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 10) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (c in 0 until 10) {
                            val pos = BoardPosition(r, c)
                            val isCorner = gameState.board.isCorner(pos)
                            val card = if (isCorner) null else gameState.board.cardAt(pos)
                            val chip = gameState.chips.at(pos)
                            val isLocked = pos in lockedPositions
                            val isValidPlacement = pos in validPlacementPositions
                            val isValidSnipe = pos in validSnipePositions
                            val isLastMove = pos == lastMovePos

                            BoardCell(
                                position = pos,
                                card = card,
                                chipTeam = chip,
                                isCorner = isCorner,
                                isLockedSequence = isLocked,
                                isValidPlacement = isValidPlacement,
                                isValidSnipeTarget = isValidSnipe,
                                isLastMove = isLastMove,
                                onClick = { onCellClick(pos) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
