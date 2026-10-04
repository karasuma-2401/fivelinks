package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.BoardGrid
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameHeader
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameOverDialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.HandView
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.RulesDialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.TacticalCraftingBar
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BackgroundDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRules by remember { mutableStateOf(false) }

    val currentPlayer = uiState.gameState.currentPlayer
    val hand = uiState.gameState.handOf(currentPlayer)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header Bar
            GameHeader(
                gameState = uiState.gameState,
                isAiThinking = uiState.isAiThinking,
                onRestartClick = { viewModel.startNewGame() },
                onRulesClick = { showRules = true },
                onMenuClick = onExitToMenu
            )

            // 2. Error Message (if any)
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TeamRed.copy(alpha = 0.9f))
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Central 10x10 Board Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BoardGrid(
                    gameState = uiState.gameState,
                    validPlacementPositions = uiState.validPlacementPositions,
                    validSnipePositions = uiState.validSnipePositions,
                    onCellClick = { pos -> viewModel.onCellClick(pos) }
                )
            }

            // 4. Tactical Action Bar (Appears when Pair, Connector, or Straight Flush selected)
            TacticalCraftingBar(
                tacticalAction = uiState.activeTacticalAction,
                onTriggerDivineWipe = { viewModel.triggerDivineWipe() }
            )

            // 5. Hand Cards Panel
            HandView(
                hand = hand,
                selectedIndices = uiState.selectedCardIndices,
                gameState = uiState.gameState,
                onCardClick = { index -> viewModel.onCardClick(index) },
                onSwapDeadCard = { index -> viewModel.swapDeadCard(index) }
            )
        }

        // 6. Game Over Modal
        if (uiState.isGameOver) {
            GameOverDialog(
                gameState = uiState.gameState,
                onRematchClick = { viewModel.startNewGame() },
                onMenuClick = onExitToMenu
            )
        }

        // 7. Rules Dialog
        if (showRules) {
            RulesDialog(onDismiss = { showRules = false })
        }
    }
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun GameScreenPreview() {
    GameScreen(
        viewModel = GameViewModel(),
        onExitToMenu = {},
        modifier = Modifier.fillMaxSize(),

    )
}