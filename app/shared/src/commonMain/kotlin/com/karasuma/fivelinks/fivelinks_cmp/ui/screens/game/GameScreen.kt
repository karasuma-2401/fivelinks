package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.karasuma.fivelinks.fivelinks_cmp.PlatformBackHandler
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.BoardGrid
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameHeader
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameNotice
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameNoticeBanner
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameOverOverlay
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.HandView
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIcon
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.MessageBox
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.RulesDialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.TacticalCraftingBar
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private enum class PendingConfirm {
    Restart,
    ExitToMenu
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRules by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }
    var notice by remember(viewModel) { mutableStateOf<GameNotice?>(null) }
    var noticeShownAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }

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
        notice = null
    }

    // Match commentary: compare each new state with the previous one.
    val lastSeenState = remember(viewModel) { mutableStateOf(uiState.gameState) }
    LaunchedEffect(uiState.gameState) {
        val next = moveNotice(lastSeenState.value, uiState.gameState, id = (notice?.id ?: 0L) + 1)
        lastSeenState.value = uiState.gameState
        if (next == null) return@LaunchedEffect
        // Let an emphasised notice (Thiên Phạt) finish before the AI's reply replaces it.
        if (notice?.emphasis == true) {
            val remaining = 2600.milliseconds - noticeShownAt.elapsedNow()
            if (remaining.isPositive()) delay(remaining)
        }
        notice = next
        noticeShownAt = TimeSource.Monotonic.markNow()
    }
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        notice = GameNotice(id = (notice?.id ?: 0L) + 1, text = message, accent = BrandRed, icon = LineIcon.Alert)
        noticeShownAt = TimeSource.Monotonic.markNow()
    }

    val gameState = uiState.gameState
    val currentPlayer = gameState.currentPlayer
    // Against the AI, keep the human's own hand on screen during the AI turn.
    val handOwner = if (currentPlayer.isAi) {
        gameState.players.firstOrNull { !it.isAi } ?: currentPlayer
    } else {
        currentPlayer
    }
    val isPlayerTurn = !currentPlayer.isAi && !uiState.isAiThinking && !uiState.isPassingTurn && !uiState.isGameOver

    // Only ask for confirmation when there is progress to lose.
    val gameInProgress = gameState.turnNumber > 0 && !uiState.isGameOver
    val requestRestart: () -> Unit = {
        if (gameInProgress) pendingConfirm = PendingConfirm.Restart else restartGame()
    }
    val requestExit: () -> Unit = {
        if (gameInProgress) pendingConfirm = PendingConfirm.ExitToMenu else onExitToMenu()
    }
    PlatformBackHandler(onBack = requestExit)

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
                onRestartClick = requestRestart,
                onRulesClick = { showRules = true },
                onMenuClick = requestExit
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
                GameNoticeBanner(
                    notice = notice,
                    modifier = Modifier.align(Alignment.TopCenter)
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

        when (pendingConfirm) {
            PendingConfirm.Restart -> MessageBox(
                title = "Chơi lại từ đầu?",
                message = "Ván đang chơi sẽ bị hủy và không thể khôi phục.",
                confirmText = "Chơi lại",
                dismissText = "Hủy",
                onConfirm = {
                    pendingConfirm = null
                    restartGame()
                },
                onDismiss = { pendingConfirm = null }
            )
            PendingConfirm.ExitToMenu -> MessageBox(
                title = "Về menu chính?",
                message = "Ván đang chơi sẽ không được lưu lại.",
                confirmText = "Về menu",
                dismissText = "Ở lại",
                onConfirm = {
                    pendingConfirm = null
                    onExitToMenu()
                },
                onDismiss = { pendingConfirm = null }
            )
            null -> Unit
        }
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
