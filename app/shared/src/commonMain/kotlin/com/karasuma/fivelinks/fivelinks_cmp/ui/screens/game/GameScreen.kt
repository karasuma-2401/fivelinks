package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.BoardGrid
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameHeader
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameOverOverlay
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.HandView
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.RulesDialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.TacticalCraftingBar
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRules by remember { mutableStateOf(false) }

    // Match clock: ticks only while the game is live and the app is in the foreground.
    var elapsedSeconds by remember(viewModel) { mutableIntStateOf(0) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val clockRunning = !uiState.isGameOver && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(viewModel, clockRunning) {
        if (!clockRunning) return@LaunchedEffect
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }
    val restartGame = {
        viewModel.startNewGame()
        elapsedSeconds = 0
    }

    val gameState = uiState.gameState
    val currentPlayer = gameState.currentPlayer
    // Against the AI, keep the human's own hand on screen during the AI turn.
    val handOwner = if (currentPlayer.isAi) {
        gameState.players.firstOrNull { !it.isAi } ?: currentPlayer
    } else {
        currentPlayer
    }
    val isPlayerTurn = !currentPlayer.isAi && !uiState.isAiThinking && !uiState.isGameOver

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                gameState = gameState,
                isAiThinking = uiState.isAiThinking,
                elapsedSeconds = elapsedSeconds,
                onRestartClick = restartGame,
                onRulesClick = { showRules = true },
                onMenuClick = onExitToMenu
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BoardGrid(
                    gameState = gameState,
                    validPlacementPositions = uiState.validPlacementPositions,
                    validSnipePositions = uiState.validSnipePositions,
                    onCellClick = { pos -> viewModel.onCellClick(pos) }
                )
                ErrorToast(
                    message = uiState.errorMessage,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                )
            }

            TacticalCraftingBar(
                tacticalAction = uiState.activeTacticalAction,
                onTriggerDivineWipe = { viewModel.triggerDivineWipe() }
            )

            HandView(
                hand = gameState.handOf(handOwner),
                owner = handOwner,
                selectedIndices = uiState.selectedCardIndices,
                gameState = gameState,
                isInteractive = isPlayerTurn,
                onCardClick = { index -> viewModel.onCardClick(index) },
                onSwapDeadCard = { index -> viewModel.swapDeadCard(index) }
            )
        }

        AnimatedVisibility(
            visible = uiState.isGameOver,
            enter = fadeIn(tween(durationMillis = 450)),
            exit = fadeOut(tween(durationMillis = 250))
        ) {
            GameOverOverlay(
                gameState = gameState,
                elapsedSeconds = elapsedSeconds,
                onRematchClick = restartGame,
                onMenuClick = onExitToMenu
            )
        }

        if (showRules) {
            RulesDialog(onDismiss = { showRules = false })
        }
    }
}

/** Floating error pill that slides in over the board and hides itself. */
@Composable
private fun ErrorToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    var visible by remember(message) { mutableStateOf(message != null) }
    var lastMessage by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        if (message != null) {
            lastMessage = message
            delay(2600)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Text(
            text = message ?: lastMessage,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PureWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(BrandRed)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun GameScreenPreview() {
    GameScreen(
        viewModel = GameViewModel(),
        onExitToMenu = {},
        modifier = Modifier.fillMaxSize()
    )
}
