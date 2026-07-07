package com.karasuma.fivelinks.fivelinks_cmp.domain

object WinDetector{
    fun detect(gameState: GameState): Team? {
        val target = gameState.config.sequenceToWin
        val candidate = gameState.config.teams.firstOrNull { gameState.sequencesOf(it) >= target }
        if (candidate != null) return candidate
        return null
    }
    // condition isDraw = no winner appear + deck no cards + hand no cards
    fun isDraw(state: GameState): Boolean = state.winner == null && state.deck.isEmpty() && state.hands.values.all { it.size == 0 }
}