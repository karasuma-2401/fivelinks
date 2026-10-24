package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SerializationCompatibilityTest {

    @Test
    fun clientMessages_serializeAndDeserializeCleanly() {
        val messages: List<ClientMessage> = listOf(
            ClientMessage.Ping(messageId = "m1", clientTime = 123456789L),
            ClientMessage.Hello(messageId = "m2", protocolVersion = ProtocolVersion.STRING, playerName = "Alice", sessionToken = "token123"),
            ClientMessage.CreateRoom(messageId = "m3", settings = RoomSettings(sequenceToWin = 2, tacticalCrafting = true, firstPlayer = FirstPlayer.RANDOM)),
            ClientMessage.JoinRoom(messageId = "m4", roomCode = "ABCDEF"),
            ClientMessage.SubmitMove(messageId = "m5", move = Move.Place("p1", Card(Suit.SPADES, Rank.ACE), BoardPosition(1, 2)), turnNumber = 3),
            ClientMessage.LeaveRoom(messageId = "m6"),
            ClientMessage.RequestRematch(messageId = "m7"),
        )

        for (msg in messages) {
            val json = ProtocolJson.encodeToString(ClientMessage.serializer(), msg)
            val decoded = ProtocolJson.decodeFromString(ClientMessage.serializer(), json)
            assertEquals(msg, decoded)
        }
    }

    @Test
    fun serverMessages_withNewErrorCodesAndCloseReasons_serializeCleanly() {
        val messages: List<ServerMessage> = listOf(
            ServerMessage.Pong(messageId = "s1", serverTime = 999999L),
            ServerMessage.Welcome(messageId = "s2", protocolVersion = ProtocolVersion.STRING, playerName = "Alice", playerId = "p_1", sessionToken = "tok"),
            ServerMessage.RoomClosed(messageId = "s3", roomCode = "ROOM1", reason = RoomCloseReason.SERVER_SHUTDOWN),
            ServerMessage.Rejected(messageId = "s4", reason = ErrorCodes.RATE_LIMITED, message = "Too fast", inReplyTo = "m5"),
            ServerMessage.Rejected(messageId = "s5", reason = ErrorCodes.SERVER_BUSY, message = "Full", inReplyTo = "m6"),
            ServerMessage.PlayerConnection(messageId = "s6", playerId = "p_1", connected = false, forfeitInMs = 60000L),
            ServerMessage.GameOver(messageId = "s7", result = GameResult(winner = Team.BLUE, reason = GameEndReason.COMPLETED_SEQUENCES)),
        )

        for (msg in messages) {
            val json = ProtocolJson.encodeToString(ServerMessage.serializer(), msg)
            val decoded = ProtocolJson.decodeFromString(ServerMessage.serializer(), json)
            assertEquals(msg, decoded)
        }
    }

    @Test
    fun gameState_roundtrip_preservesBoardAndChips() {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = 777L)
        val players = listOf(
            Player(id = "p1", name = "Player 1", team = Team.BLUE),
            Player(id = "p2", name = "Player 2", team = Team.RED),
        )
        val state = GameEngine.initialize(config, players)
        val json = ProtocolJson.encodeToString(state)
        val decoded = ProtocolJson.decodeFromString<com.karasuma.fivelinks.fivelinks_cmp.domain.GameState>(json)

        assertEquals(state.turnNumber, decoded.turnNumber)
        assertEquals(state.currentPlayer.id, decoded.currentPlayer.id)
        assertEquals(state.board.cells.size, decoded.board.cells.size)
        assertEquals(state.deck.cards.size, decoded.deck.cards.size)
    }

    @Test
    fun unknownKeysInJson_areIgnoredGracefully() {
        val jsonWithExtra = """
            {
                "type": "ping",
                "messageId": "p99",
                "clientTime": 1000,
                "extraTelemetryField": "ignored_value",
                "futureVersionPayload": [1, 2, 3]
            }
        """.trimIndent()

        val decoded = ProtocolJson.decodeFromString(ClientMessage.serializer(), jsonWithExtra)
        assertTrue(decoded is ClientMessage.Ping)
        assertEquals("p99", decoded.messageId)
        assertEquals(1000L, decoded.clientTime)
    }
}

