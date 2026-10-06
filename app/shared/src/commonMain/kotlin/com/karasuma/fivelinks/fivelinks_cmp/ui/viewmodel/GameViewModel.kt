package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.Dispatchers
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
    private val aiDifficulty: Difficulty = Difficulty.MEDIUM
) : ViewModel(), GameController {

    private val aiEvaluator = HeuristicEvaluator()

    private val _uiState = MutableStateFlow(
        createInitialState(initialConfig, initialPlayers)
    )
    override val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private fun createInitialState(config: GameConfig, players: List<Player>): GameUiState {
        val state = GameEngine.initialize(config, players)
        return GameUiState(
            gameState = state,
            isGameOver = state.isGameOver
        )
    }

    override fun startNewGame() {
        val previous = _uiState.value.gameState
        // New seed so a rematch deals a different board and hands.
        _uiState.value = createInitialState(previous.config.copy(seed = Random.nextLong()), previous.players)
        checkAndTriggerAi()
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
        val tactical = current.activeTacticalAction
        if (tactical is TacticalAction.DivineWipe) {
            val player = current.gameState.currentPlayer
            val move = Move.DivineWipe(player.id, tactical.cards)
            applyPlayerMove(move)
        }
    }

    override fun swapDeadCard(index: Int) {
        val current = _uiState.value
        val player = current.gameState.currentPlayer
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
        viewModelScope.launch {
            delay(1000)
            // A new game may have started in the meantime.
            if (_uiState.value.gameState !== stuck) return@launch
            _uiState.update {
                it.copy(gameState = skipped.state, isGameOver = skipped.state.isGameOver, isPassingTurn = false)
            }
            checkAndTriggerAi(aiDelayMillis = 1100) // Leave the pass notice up a little longer
        }
        return true
    }

    private fun checkAndTriggerAi(aiDelayMillis: Long = 450) {
        if (passIfStuck()) return
        val current = _uiState.value
        if (current.isGameOver) return

        val currentPlayer = current.gameState.currentPlayer
        if (currentPlayer.isAi) {
            viewModelScope.launch {
                _uiState.update { it.copy(isAiThinking = true) }
                delay(aiDelayMillis) // Subtle delay for smooth human-friendly interaction

                val aiMove = withContext(Dispatchers.Default) {
                    runCatching {
                        aiEvaluator.chooseMove(
                            state = _uiState.value.gameState,
                            playerId = currentPlayer.id,
                            difficulty = aiDifficulty
                        )
                    }.getOrNull()
                }

                if (aiMove != null) {
                    val nextResult = GameEngine.applyMove(_uiState.value.gameState, aiMove)
                    if (nextResult.isSuccess) {
                        val nextState = nextResult.getOrThrow()
                        _uiState.update {
                            it.copy(
                                gameState = nextState,
                                isAiThinking = false,
                                isGameOver = nextState.isGameOver
                            )
                        }
                        // If following player is also AI, recurse
                        checkAndTriggerAi()
                        return@launch
                    }
                }
                _uiState.update { it.copy(isAiThinking = false) }
            }
        }
    }
}
