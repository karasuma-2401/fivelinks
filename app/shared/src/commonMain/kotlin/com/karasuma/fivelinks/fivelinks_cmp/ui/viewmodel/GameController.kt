package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import kotlinx.coroutines.flow.StateFlow

/** What the game screen drives, whether the game runs on this device or on the server. */
interface GameController {
    val uiState: StateFlow<GameUiState>

    fun onCardClick(index: Int)

    fun onCellClick(position: BoardPosition)

    fun triggerDivineWipe()

    fun swapDeadCard(index: Int)

    fun reorderHand(fromIndex: Int, toIndex: Int) = Unit

    /** Offline: deal a new game. Online: ask the opponent for a rematch. */
    fun startNewGame()

    /** Release work owned by this controller when its screen is left. */
    fun close() = Unit
}
