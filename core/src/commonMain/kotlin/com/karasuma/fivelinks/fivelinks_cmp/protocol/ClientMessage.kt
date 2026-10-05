package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ClientMessage {
    val messageId: String
    @Serializable
    @SerialName("hello")
    data class Hello(
        override val messageId: String,
        val protocolVersion: String = ProtocolVersion.STRING,
        val playerName: String,
        /** From an earlier [ServerMessage.Welcome]: resumes that session (and its room) after a reconnect. */
        val sessionToken: String? = null,
    ) : ClientMessage

    @Serializable
    @SerialName("ping")
    data class Ping(
        override val messageId: String,
        val clientTime: Long
    ) : ClientMessage

    @Serializable
    @SerialName("create_room")
    data class CreateRoom(
        override val messageId: String,
        val settings: RoomSettings = RoomSettings(),
    ) : ClientMessage

    @Serializable
    @SerialName("join_room")
    data class JoinRoom(
        override val messageId: String,
        val roomCode: String,
    ) : ClientMessage

    /** [turnNumber] is the turn the move was made on; a late or repeated submit is rejected. */
    @Serializable
    @SerialName("submit_move")
    data class SubmitMove(
        override val messageId: String,
        val move: Move,
        val turnNumber: Int,
    ) : ClientMessage

    /** Leaving a game in progress forfeits it. */
    @Serializable
    @SerialName("leave_room")
    data class LeaveRoom(
        override val messageId: String,
    ) : ClientMessage

    /** A new game starts once both players have asked. */
    @Serializable
    @SerialName("request_rematch")
    data class RequestRematch(
        override val messageId: String,
    ) : ClientMessage
}
