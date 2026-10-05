package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolMessagesTest {

    private val state = GameEngine.initialize(
        GameConfig.tactical(playerCount = 2, teams = 2, seed = 11L),
        listOf(Player("host", "Host", Team.BLUE), Player("guest", "Guest", Team.RED)),
    )
    private val firstMove = GameEngine.legalMoves(state, "host").first()

    private fun roundTrip(message: ClientMessage): String {
        val json = ProtocolJson.encodeToString(ClientMessage.serializer(), message)
        assertEquals(message, ProtocolJson.decodeFromString(ClientMessage.serializer(), json))
        return json
    }

    private fun roundTrip(message: ServerMessage): String {
        val json = ProtocolJson.encodeToString(ServerMessage.serializer(), message)
        assertEquals(message, ProtocolJson.decodeFromString(ServerMessage.serializer(), json))
        return json
    }

    private fun typeOf(json: String): String =
        ProtocolJson.parseToJsonElement(json).jsonObject.getValue("type").jsonPrimitive.content

    @Test
    fun clientMessages_roundTrip() {
        val messages = mapOf(
            "hello" to ClientMessage.Hello("1", playerName = "An", sessionToken = "token-1"),
            "ping" to ClientMessage.Ping("2", clientTime = 1_700_000_000_000),
            "create_room" to ClientMessage.CreateRoom("3", RoomSettings(sequenceToWin = 1, tacticalCrafting = false)),
            "join_room" to ClientMessage.JoinRoom("4", roomCode = "K7QP2M"),
            "submit_move" to ClientMessage.SubmitMove("5", move = firstMove, turnNumber = 0),
            "leave_room" to ClientMessage.LeaveRoom("6"),
            "request_rematch" to ClientMessage.RequestRematch("7"),
        )
        for ((type, message) in messages) assertEquals(type, typeOf(roundTrip(message)))
    }

    @Test
    fun serverMessages_roundTrip() {
        val view = state.viewFor("guest")
        val messages = mapOf(
            "Welcome" to ServerMessage.Welcome("1", ProtocolVersion.STRING, "An", playerId = "host", sessionToken = "token-1"),
            "pong" to ServerMessage.Pong("2", serverTime = 1_700_000_000_000),
            "rejected" to ServerMessage.Rejected("3", ErrorCodes.ROOM_FULL, "Room is full", inReplyTo = "4"),
            "room_state" to ServerMessage.RoomState(
                "4", "K7QP2M", RoomSettings(), RoomStatus.WAITING,
                listOf(RoomPlayer("host", "Host", Team.BLUE), RoomPlayer("guest", "Guest", Team.RED, connected = false)),
            ),
            "game_snapshot" to ServerMessage.GameSnapshot("5", "K7QP2M", Board.standard, view, turnTimeLeftMs = 60_000),
            "state_update" to ServerMessage.StateUpdate(
                "6", view,
                listOf(
                    GameEvent.MovePlayed("host", firstMove),
                    GameEvent.TurnPassed("guest", PassReason.TIMEOUT),
                ),
                turnTimeLeftMs = 59_000,
            ),
            "player_connection" to ServerMessage.PlayerConnection("7", "guest", connected = false, forfeitInMs = 60_000),
            "game_over" to ServerMessage.GameOver("8", GameResult(Team.BLUE, GameEndReason.FORFEIT_DISCONNECTED)),
        )
        for ((type, message) in messages) assertEquals(type, typeOf(roundTrip(message)))
    }

    @Test
    fun events_carryTheirOwnType() {
        val json = roundTrip(ServerMessage.StateUpdate("1", state.viewFor("host"), listOf(GameEvent.TurnPassed("host", PassReason.NO_LEGAL_MOVES)), 0))
        assertTrue("\"turn_passed\"" in json)
        assertTrue("\"NO_LEGAL_MOVES\"" in json)
    }

    @Test
    fun version1_0Messages_stillDecode() {
        // Fields added in 1.1 are optional, so 1.0 payloads keep working.
        val hello = ProtocolJson.decodeFromString(
            ClientMessage.serializer(),
            """{"type":"hello","messageId":"a","protocolVersion":"1.0","playerName":"An"}""",
        ) as ClientMessage.Hello
        assertNull(hello.sessionToken)

        val welcome = ProtocolJson.decodeFromString(
            ServerMessage.serializer(),
            """{"type":"Welcome","messageId":"b","protocolVersion":"1.0","playerName":"An"}""",
        ) as ServerMessage.Welcome
        assertEquals("", welcome.playerId)

        val rejected = ProtocolJson.decodeFromString(
            ServerMessage.serializer(),
            """{"type":"rejected","messageId":"c","reason":"INTERNAL_ERROR","message":"x"}""",
        ) as ServerMessage.Rejected
        assertNull(rejected.inReplyTo)
    }

    @Test
    fun version_isMinorBump() {
        assertEquals("1.1", ProtocolVersion.STRING)
        assertTrue(ProtocolVersion.majorCompatible("1.0"))
        assertTrue(!ProtocolVersion.majorCompatible("2.0"))
    }
}
