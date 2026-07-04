package com.karasuma.fivelinks.fivelinks_cmp.protocol

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
        val playerName: String
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
        val message: String
    ) : ServerMessage
}