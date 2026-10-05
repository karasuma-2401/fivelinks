package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.TacticalPatterns
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.isOneEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.domain.isTwoEyedJack
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
) : ViewModel() {

    private val aiEvaluator = HeuristicEvaluator()

    private val _uiState = MutableStateFlow(
        createInitialState(initialConfig, initialPlayers)
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private fun createInitialState(config: GameConfig, players: List<Player>): GameUiState {
        val state = GameEngine.initialize(config, players)
        return GameUiState(
            gameState = state,
            isGameOver = state.isGameOver
        )
    }

    fun startNewGame(
        // New seed so a rematch deals a different board and hands.
        config: GameConfig = _uiState.value.gameState.config.copy(seed = Random.nextLong()),
        players: List<Player> = _uiState.value.gameState.players
    ) {
        _uiState.value = createInitialState(config, players)
        checkAndTriggerAi()
    }

    fun onCardClick(index: Int) {
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
        val player = state.currentPlayer
        val hand = state.handOf(player)
        val selectedCards = selectedIndices.map { hand[it] }

        var activeTactical: TacticalAction? = null
        val validPlacements = mutableSetOf<BoardPosition>()
        val validSnipes = mutableSetOf<BoardPosition>()

        if (state.config.enableTacticalCrafting) {
            when {
                // 5 cards Straight Flush check
                selectedCards.size == 5 && TacticalPatterns.isStraightFlush(selectedCards) -> {
                    activeTactical = TacticalAction.DivineWipe(selectedCards)
                    // Highlight all opponent chips to signal wipe target
                    for (pos in BoardPosition.all) {
                        val chip = state.chips.at(pos)
                        if (chip != null && chip != player.team) {
                            validSnipes.add(pos)
                        }
                    }
                }
                // 2 cards Tactical Crafting check
                selectedCards.size == 2 -> {
                    val c1 = selectedCards[0]
                    val c2 = selectedCards[1]
                    if (TacticalPatterns.isPair(c1, c2)) {
                        activeTactical = TacticalAction.PairWild(c1, c2)
                        // Wild Place: all open non-corner cells
                        for (pos in BoardPosition.all) {
                            if (!state.board.isCorner(pos) && !state.chips.contains(pos)) {
                                validPlacements.add(pos)
                            }
                        }
                    } else if (TacticalPatterns.isSuitedConnector(c1, c2)) {
                        activeTactical = TacticalAction.ConnectorSnipe(c1, c2)
                        // Wild Snipe: all opponent chips not locked in sequence
                        for (pos in BoardPosition.all) {
                            val chip = state.chips.at(pos)
                            val isLocked = state.completedSequence.any { pos in it.positions }
                            if (chip != null && chip != player.team && !isLocked) {
                                validSnipes.add(pos)
                            }
                        }
                    }
                }
            }
        }

        // Single Card Classic Play
        if (selectedCards.size == 1 && activeTactical == null) {
            val card = selectedCards.first()
            when {
                card.isTwoEyedJack() -> {
                    for (pos in BoardPosition.all) {
                        if (!state.board.isCorner(pos) && !state.chips.contains(pos)) {
                            validPlacements.add(pos)
                        }
                    }
                }
                card.isOneEyedJack() -> {
                    for (pos in BoardPosition.all) {
                        val chip = state.chips.at(pos)
                        val isLocked = state.completedSequence.any { pos in it.positions }
                        if (chip != null && chip != player.team && !isLocked) {
                            validSnipes.add(pos)
                        }
                    }
                }
                else -> {
                    val openPositions = state.board.positionsOf(card).filter { !state.chips.contains(it) }
                    validPlacements.addAll(openPositions)
                }
            }
        }

        _uiState.update {
            it.copy(
                selectedCardIndices = selectedIndices,
                activeTacticalAction = activeTactical,
                validPlacementPositions = validPlacements,
                validSnipePositions = validSnipes,
                errorMessage = null
            )
        }
    }

    fun onCellClick(position: BoardPosition) {
        val current = _uiState.value
        if (current.isAiThinking || current.isPassingTurn || current.isGameOver) return

        val player = current.gameState.currentPlayer
        if (player.isAi) return

        val tactical = current.activeTacticalAction
        val selectedIndices = current.selectedCardIndices
        val hand = current.gameState.handOf(player)

        val move: Move? = when {
            // Tactical: Pair Wild Place
            tactical is TacticalAction.PairWild -> {
                if (position in current.validPlacementPositions) {
                    Move.CraftPlace(player.id, tactical.card1, tactical.card2, position)
                } else null
            }
            // Tactical: Connector Snipe Remove
            tactical is TacticalAction.ConnectorSnipe -> {
                if (position in current.validSnipePositions) {
                    Move.CraftRemove(player.id, tactical.card1, tactical.card2, position)
                } else null
            }
            // Classic: Single Card Move
            selectedIndices.size == 1 -> {
                val card = hand[selectedIndices.first()]
                when {
                    card.isOneEyedJack() -> {
                        if (position in current.validSnipePositions) {
                            Move.Remove(player.id, card, position)
                        } else null
                    }
                    else -> {
                        if (position in current.validPlacementPositions) {
                            Move.Place(player.id, card, position)
                        } else null
                    }
                }
            }
            else -> null
        }

        if (move != null) {
            applyPlayerMove(move)
        }
    }

    fun triggerDivineWipe() {
        val current = _uiState.value
        val tactical = current.activeTacticalAction
        if (tactical is TacticalAction.DivineWipe) {
            val player = current.gameState.currentPlayer
            val move = Move.DivineWipe(player.id, tactical.cards)
            applyPlayerMove(move)
        }
    }

    fun swapDeadCard(index: Int) {
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

/**
 * The engine reports rule violations in English (it is shared with the server),
 * so translate the ones a player can trigger before showing them.
 */
private fun localizeMoveError(message: String?): String {
    val text = message.orEmpty()
    return when {
        text.startsWith("Game is over") -> "Ván đấu đã kết thúc."
        text.startsWith("It's not your turn") -> "Chưa đến lượt của bạn."
        text.startsWith("Card not in hand") -> "Lá bài này không có trên tay."
        text.startsWith("Cannot place on a corner") -> "Không thể đặt quân vào ô góc."
        text.startsWith("Position is already occupied") -> "Ô này đã có quân."
        text.startsWith("Cannot place a one-eyed jack") -> "J 1 mắt không dùng để đặt quân."
        text.startsWith("Cannot place a card that is not the same") -> "Lá bài không khớp với ô trên bàn cờ."
        text.startsWith("Remove requires a one-eyed Jack") -> "Cần J 1 mắt để gỡ quân."
        text.startsWith("no chip at") -> "Ô này không có quân để gỡ."
        text.startsWith("cannot remove own team's chip") -> "Không thể gỡ quân của đội mình."
        text.startsWith("cannot remove a chip that is locked") -> "Quân đã khóa trong hàng, không thể gỡ."
        text.startsWith("Jacks are never dead") -> "Lá J không bao giờ là bài chết."
        text.endsWith("is not on the board") -> "Lá bài này không có trên bàn cờ."
        text.endsWith("is not dead") -> "Lá bài này chưa phải bài chết."
        text.startsWith("cannot swap") -> "Hết bài để đổi."
        text.startsWith("Tactical crafting is disabled") -> "Chế độ chiến thuật đang tắt."
        text.startsWith("Player does not hold both") -> "Bạn không có đủ 2 lá để ghép."
        text.startsWith("CraftPlace requires") -> "Ghép đôi cần 2 lá cùng số."
        text.startsWith("CraftRemove requires") -> "Cần 2 lá liên tiếp cùng chất."
        text.startsWith("DivineWipe requires exactly") -> "Thiên Phạt cần đúng 5 lá."
        text.startsWith("Player does not hold all 5") -> "Bạn không có đủ 5 lá."
        text.startsWith("DivineWipe requires") -> "Thiên Phạt cần sảnh đồng chất 5 lá."
        else -> "Nước đi không hợp lệ!"
    }
}
