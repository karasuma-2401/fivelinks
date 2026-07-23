package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val schemaVersion: Int = SCHEMA_VERSION,
    val config: GameConfig,
    val players: List<Player>,
    val board: Board,
    val chips: ChipMap,
    val hands: Map<PlayerId, Hand>,
    val deck: Deck,
    val discard: List<Card>,
    val currentPlayerIndex: Int,
    val completedSequence: List<ChipSequence> = emptyList(),
    val winner: Team? = null,
    val turnNumber: Int = 0,
    val lastMove: Move? = null,
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]

    val isGameOver: Boolean get() = winner != null

    fun handOf(player: Player): Hand = hands[player.id]!!

    fun sequencesOf(team: Team): Int = completedSequence.filter { it.team == team }.size
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
