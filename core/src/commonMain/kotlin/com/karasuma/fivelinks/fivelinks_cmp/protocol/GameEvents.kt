package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What happened between two [ServerMessage.StateUpdate]s, in order. */
@Serializable
sealed interface GameEvent {
    @Serializable
    @SerialName("move_played")
    data class MovePlayed(val playerId: PlayerId, val move: Move) : GameEvent

    @Serializable
    @SerialName("turn_passed")
    data class TurnPassed(val playerId: PlayerId, val reason: PassReason) : GameEvent
}

@Serializable
enum class PassReason {
    /** Nothing playable: the deck is empty and no card in hand fits the board. */
    NO_LEGAL_MOVES,
    /** The turn timer ran out. */
    TIMEOUT,
}

/** How a game ended; [winner] is null for a draw. */
@Serializable
data class GameResult(
    val winner: Team?,
    val reason: GameEndReason,
)

@Serializable
enum class GameEndReason {
    COMPLETED_SEQUENCES,
    /** Nobody could move any more. */
    DRAW_NO_MOVES,
    /** The loser stayed disconnected for longer than the grace period. */
    FORFEIT_DISCONNECTED,
    /** The loser left the room. */
    FORFEIT_LEFT,
}
