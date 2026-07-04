package com.karasuma.fivelinks.fivelinks_cmp.protocol

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
        val playerName: String
    ) : ClientMessage

    @Serializable
    @SerialName("ping")
    data class Ping(
        override val messageId: String,
        val clientTime: Long
    ) : ClientMessage
}