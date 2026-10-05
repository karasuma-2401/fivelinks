package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipMap
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipSequence
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.serialization.Serializable

/**
 * What one player may see of an online game: everything on the table plus their own
 * hand. Opponents' hands, the deck order and the shuffle seed (which would reveal the
 * deck) never leave the server. The board layout is sent once, in [ServerMessage.GameStarted].
 */
@Serializable
data class PlayerView(
    val viewerId: PlayerId,
    /** The game's settings, with `seed` zeroed. */
    val config: GameConfig,
    val players: List<Player>,
    val chips: ChipMap,
    /** The viewer's own cards. */
    val hand: Hand,
    /** Cards held by every player, the viewer included. */
    val handSizes: Map<PlayerId, Int>,
    val deckSize: Int,
    val discard: List<Card>,
    val currentPlayerIndex: Int,
    val completedSequence: List<ChipSequence>,
    val winner: Team? = null,
    val isDraw: Boolean = false,
    val turnNumber: Int,
    val lastMove: Move? = null,
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]
    val isGameOver: Boolean get() = winner != null || isDraw
}

/** The part of this game that [playerId] is allowed to see. */
fun GameState.viewFor(playerId: PlayerId): PlayerView {
    val viewer = players.firstOrNull { it.id == playerId }
    require(viewer != null) { "$playerId is not in this game" }
    return PlayerView(
        viewerId = playerId,
        config = config.copy(seed = 0),
        players = players,
        chips = chips,
        hand = handOf(viewer),
        handSizes = hands.mapValues { (_, hand) -> hand.size },
        deckSize = deck.size,
        discard = discard,
        currentPlayerIndex = currentPlayerIndex,
        completedSequence = completedSequence,
        winner = winner,
        isDraw = isDraw,
        turnNumber = turnNumber,
        lastMove = lastMove,
    )
}
