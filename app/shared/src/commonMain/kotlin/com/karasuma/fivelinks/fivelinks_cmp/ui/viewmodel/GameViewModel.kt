package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.ai.AiService
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GameViewModel(
    initialConfig: GameConfig = GameConfig.tactical(playerCount = 2, teams = 2),
    initialPlayers: List<Player> = listOf(
        Player(id = "p0", name = "Người chơi 1", team = Team.BLUE, isAi = false),
        Player(id = "p1", name = "AI", team = Team.RED, isAi = true)
    ),
    val aiDifficulty: Difficulty = Difficulty.MEDIUM,
    private val aiService: AiService = AiFacadeService(),
    private val heuristicFallback: AiService = HeuristicEvaluator(),
    private val aiDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val aiThinkDelayMillis: Long = AI_THINK_DELAY_MS,
    private val passDelayMillis: Long = PASS_DELAY_MS,
    restoredState: GameState? = null,
    private val onStateChanged: ((GameState) -> Unit)? = null,
) : ViewModel(), GameController {
    private val _uiState = MutableStateFlow(
        if (restoredState != null) {
            GameUiState(gameState = restoredState, isGameOver = restoredState.isGameOver)
        } else {
            createInitialState(initialConfig, initialPlayers)
        }
    )
    override val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var aiJob: Job? = null
    private var passJob: Job? = null
    /** Invalidates a completed-but-stale search before it can change a newer game. */
    private var gameGeneration = 0L

    init {
        checkAndTriggerAi()
    }

    private fun createInitialState(config: GameConfig, players: List<Player>): GameUiState {
        val state = GameEngine.initialize(config, players)
        return GameUiState(
            gameState = state,
            isGameOver = state.isGameOver
        )
    }

    override fun startNewGame() {
        val previous = _uiState.value.gameState
        cancelPendingWork()
        gameGeneration++
        // New seed so a rematch deals a different board and hands.
        val newState = createInitialState(previous.config.copy(seed = Random.nextLong()), previous.players)
        _uiState.value = newState
        onStateChanged?.invoke(newState.gameState)
        checkAndTriggerAi()
    }

    override fun close() {
        cancelPendingWork()
        gameGeneration++
        _uiState.update { it.copy(isAiThinking = false, isPassingTurn = false) }
        onStateChanged?.invoke(_uiState.value.gameState)
    }

    override fun onCleared() {
        close()
        super.onCleared()
    }

    override fun onCardClick(index: Int) {
        val current = _uiState.value
        if (current.isAiThinking || current.isPassingTurn || current.isGameOver) return

        val currentPlayer = current.gameState.currentPlayer
        if (currentPlayer.isAi) return

        val hand = current.gameState.handOf(currentPlayer)
        if (index !in 0 until hand.size) return

        val newSelection = if (index in current.selectedCardIndices) {
            current.selectedCardIndices - index
        } else {
            current.selectedCardIndices + index
        }

        recalculateHighlights(newSelection)
    }

    private fun recalculateHighlights(selectedIndices: Set<Int>) {
        val state = _uiState.value.gameState
        val selection = cardSelection(state, state.currentPlayer, selectedIndices)
        _uiState.update {
            it.copy(
                selectedCardIndices = selection.indices,
                activeTacticalAction = selection.tacticalAction,
                validPlacementPositions = selection.placements,
                validSnipePositions = selection.snipes,
                errorMessage = null
            )
        }
    }

    override fun onCellClick(position: BoardPosition) {
        val current = _uiState.value
        if (current.isAiThinking || current.isPassingTurn || current.isGameOver) return

        val player = current.gameState.currentPlayer
        if (player.isAi) return

        val move = moveForTap(current.gameState, player, current.selection, position)
        if (move != null) {
            applyPlayerMove(move)
        }
    }

    override fun triggerDivineWipe() {
        val current = _uiState.value
        if (current.isAiThinking || current.isPassingTurn || current.isGameOver) return
        val tactical = current.activeTacticalAction
        if (tactical is TacticalAction.DivineWipe) {
            val player = current.gameState.currentPlayer
            if (player.isAi) return
            val move = Move.DivineWipe(player.id, tactical.cards)
            applyPlayerMove(move)
        }
    }

    override fun swapDeadCard(index: Int) {
        val current = _uiState.value
        if (current.isAiThinking || current.isPassingTurn || current.isGameOver) return
        val player = current.gameState.currentPlayer
        if (player.isAi) return
        val hand = current.gameState.handOf(player)
        if (index !in 0 until hand.size) return

        val card = hand[index]
        val move = Move.SwapDeadCard(player.id, card)
        applyPlayerMove(move)
    }

    private fun applyPlayerMove(move: Move) {
        val result = GameEngine.applyMove(_uiState.value.gameState, move)
        if (result.isSuccess) {
            val nextState = result.getOrThrow()
            gameGeneration++
            _uiState.update {
                it.copy(
                    gameState = nextState,
                    selectedCardIndices = emptySet(),
                    activeTacticalAction = null,
                    validPlacementPositions = emptySet(),
                    validSnipePositions = emptySet(),
                    errorMessage = null,
                    isGameOver = nextState.isGameOver
                )
            }
            onStateChanged?.invoke(nextState)
            checkAndTriggerAi()
        } else {
            _uiState.update {
                it.copy(errorMessage = localizeMoveError(result.exceptionOrNull()?.message))
            }
        }
    }

    /**
     * A player with nothing playable passes; if nobody can play, the game is a draw.
     * The pass gets a beat of its own, like an AI move, so the screen can announce it
     * after the move before it. Returns false when the player to move can play.
     */
    private fun passIfStuck(): Boolean {
        val stuck = _uiState.value.gameState
        val skipped = GameEngine.skipStuckTurns(stuck)
        if (skipped.passed.isEmpty()) return false

        _uiState.update { it.copy(isPassingTurn = true) }
        val generation = gameGeneration
        passJob?.cancel()
        passJob = viewModelScope.launch {
            delay(passDelayMillis)
            // A new game may have started in the meantime.
            if (!isCurrentGame(generation, stuck)) return@launch
            passJob = null
            gameGeneration++
            _uiState.update {
                it.copy(gameState = skipped.state, isGameOver = skipped.state.isGameOver, isPassingTurn = false)
            }
            onStateChanged?.invoke(skipped.state)
            checkAndTriggerAi()
        }
        return true
    }

    private fun checkAndTriggerAi() {
        if (passIfStuck()) return
        val current = _uiState.value
        if (current.isGameOver) return

        val currentPlayer = current.gameState.currentPlayer
        if (!currentPlayer.isAi || aiJob?.isActive == true) return

        val generation = gameGeneration
        val stateAtTurnStart = current.gameState
        val playerId = currentPlayer.id
        aiJob = viewModelScope.launch {
            _uiState.update { state ->
                if (isCurrentAiTurn(generation, stateAtTurnStart, playerId)) {
                    state.copy(isAiThinking = true, errorMessage = null)
                } else {
                    state
                }
            }
            delay(aiThinkDelayMillis) // A short beat keeps the response readable without blocking UI.
            if (!isCurrentAiTurn(generation, stateAtTurnStart, playerId)) return@launch

            val aiMove = chooseAiMove(stateAtTurnStart, playerId)
            if (!isCurrentAiTurn(generation, stateAtTurnStart, playerId)) return@launch

            val nextState = aiMove
                ?.let { move -> GameEngine.applyMove(stateAtTurnStart, move).getOrNull() }
            if (nextState != null) {
                aiJob = null
                gameGeneration++
                _uiState.update {
                    it.copy(
                        gameState = nextState,
                        selectedCardIndices = emptySet(),
                        activeTacticalAction = null,
                        validPlacementPositions = emptySet(),
                        validSnipePositions = emptySet(),
                        isAiThinking = false,
                        isGameOver = nextState.isGameOver,
                    )
                }
                onStateChanged?.invoke(nextState)
                // If the following player is also AI, schedule their independent turn.
                checkAndTriggerAi()
            } else {
                aiJob = null
                _uiState.update { state ->
                    if (isCurrentAiTurn(generation, stateAtTurnStart, playerId)) {
                        state.copy(isAiThinking = false, errorMessage = AI_MOVE_FAILED)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private suspend fun chooseAiMove(state: GameState, playerId: PlayerId): Move? =
        withContext(aiDispatcher) {
            try {
                aiService.chooseMove(state, playerId, aiDifficulty)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                try {
                    heuristicFallback.chooseMove(state, playerId, aiDifficulty)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }

    private fun isCurrentGame(generation: Long, state: GameState): Boolean =
        gameGeneration == generation && _uiState.value.gameState === state

    private fun isCurrentAiTurn(
        generation: Long,
        state: GameState,
        playerId: PlayerId,
    ): Boolean = isCurrentGame(generation, state) &&
        _uiState.value.gameState.currentPlayer.id == playerId

    private fun cancelPendingWork() {
        aiJob?.cancel()
        aiJob = null
        passJob?.cancel()
        passJob = null
    }

    private companion object {
        const val AI_THINK_DELAY_MS = 450L
        const val PASS_DELAY_MS = 1_000L
        const val AI_MOVE_FAILED = "AI chưa thể chọn nước đi. Bạn hãy chơi lại ván mới."
    }
}
