package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ServerMessage {
    val messageId: String

    @Serializable
    @SerialName("Welcome")
    data class Welcome(
        override val messageId: String,
        val protocolVersion: String,
        val playerName: String,
        /** This player's id for the whole session. */
        val playerId: PlayerId = "",
        /** Send back in [ClientMessage.Hello] to resume after a reconnect. */
        val sessionToken: String = "",
    ) : ServerMessage

    @Serializable
    @SerialName("pong")
    data class Pong(
        override val messageId: String,
        val serverTime: Long
    ) : ServerMessage

    @Serializable
    @SerialName("rejected")
    data class Rejected(
        override val messageId: String,
        val reason: ErrorCodes,
        val message: String,
        /** The messageId of the client message that was refused, if any. */
        val inReplyTo: String? = null,
    ) : ServerMessage

    /** Who is in the room and what stage it is at; sent on every change. */
    @Serializable
    @SerialName("room_state")
    data class RoomState(
        override val messageId: String,
        val roomCode: String,
        val settings: RoomSettings,
        val status: RoomStatus,
        val players: List<RoomPlayer>,
    ) : ServerMessage

    /** Full sync: sent when a game starts and when a player reconnects mid-game. */
    @Serializable
    @SerialName("game_snapshot")
    data class GameSnapshot(
        override val messageId: String,
        val roomCode: String,
        val board: Board,
        val view: PlayerView,
        /** Time left in the current turn when this was sent. */
        val turnTimeLeftMs: Long,
    ) : ServerMessage

    @Serializable
    @SerialName("state_update")
    data class StateUpdate(
        override val messageId: String,
        val view: PlayerView,
        val events: List<GameEvent>,
        /** Time left in the current turn when this was sent. */
        val turnTimeLeftMs: Long,
    ) : ServerMessage

    /** A player dropped or came back; while they are away, [forfeitInMs] counts down to a forfeit. */
    @Serializable
    @SerialName("player_connection")
    data class PlayerConnection(
        override val messageId: String,
        val playerId: PlayerId,
        val connected: Boolean,
        val forfeitInMs: Long? = null,
    ) : ServerMessage

    /** Follows the final [StateUpdate] of a game. */
    @Serializable
    @SerialName("game_over")
    data class GameOver(
        override val messageId: String,
        val result: GameResult,
    ) : ServerMessage
}
