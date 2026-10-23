package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import kotlin.time.ComparableTimeMark

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
    val isGameOver: Boolean = false,
    /** Set for a server-authoritative game. The local player always sees their own hand. */
    val localPlayerId: PlayerId? = null,
    /** The authoritative result, including forfeits which are not always present in [gameState]. */
    val gameResult: GameResult? = null,
    /** Server turn deadline on this device's monotonic clock; null for offline games. */
    val turnEndsAt: ComparableTimeMark? = null,
    /** Offline games may restart at any point; online games may request a rematch only after ending. */
    val canRestart: Boolean = true,
    /** The local player has already asked the opponent for an online rematch. */
    val isRematchPending: Boolean = false,
    /** Online-only status while the other player has a chance to reconnect. */
    val opponentDisconnected: Boolean = false,
    /** When the disconnected opponent forfeits, on this device's monotonic clock. */
    val opponentForfeitAt: ComparableTimeMark? = null,
) {
    val selection: CardSelection
        get() = CardSelection(selectedCardIndices, activeTacticalAction, validPlacementPositions, validSnipePositions)
}
