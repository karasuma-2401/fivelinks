package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState

sealed interface TacticalAction {
    data class PairWild(val card1: Card, val card2: Card) : TacticalAction
    data class ConnectorSnipe(val card1: Card, val card2: Card) : TacticalAction
    data class DivineWipe(val cards: List<Card>) : TacticalAction
}

data class GameUiState(
    val gameState: GameState,
    val selectedCardIndices: Set<Int> = emptySet(),
    val activeTacticalAction: TacticalAction? = null,
    val validPlacementPositions: Set<BoardPosition> = emptySet(),
    val validSnipePositions: Set<BoardPosition> = emptySet(),
    val isAiThinking: Boolean = false,
    /** The player to move has nothing playable and is about to pass. */
    val isPassingTurn: Boolean = false,
    val errorMessage: String? = null,
    val isGameOver: Boolean = false
)
