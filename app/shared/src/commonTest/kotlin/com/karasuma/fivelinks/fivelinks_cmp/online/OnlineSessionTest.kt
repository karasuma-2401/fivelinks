package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import com.karasuma.fivelinks.fivelinks_cmp.protocol.FirstPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEndReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEvent
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomCloseReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomStatus
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.viewFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class) // the test scheduler's time source
class OnlineSessionTest {

    private val connection = FakeConnection()

    private fun TestScope.startedSession(): OnlineSession {
        val session = OnlineSession(connection, backgroundScope, testScheduler.timeSource)
        session.start()
        connection.state.value = ConnectionState.Connected(ME)
        connection.incoming.tryEmit(ServerMessage.Welcome("s0", "1.1", "Me", playerId = ME, sessionToken = "token"))
        runCurrent()
        return session
    }

    private fun TestScope.receive(vararg messages: ServerMessage) {
        messages.forEach { check(connection.incoming.tryEmit(it)) }
        runCurrent()
    }

    private fun roomState(status: RoomStatus, youConnected: Boolean = true) = ServerMessage.RoomState(
        "r", "K7QP2M", RoomSettings(), status,
        listOf(RoomPlayer(ME, "Me", Team.BLUE), RoomPlayer(YOU, "You", Team.RED, connected = youConnected)),
    )

    private fun snapshot(state: GameState) = ServerMessage.GameSnapshot("g", "K7QP2M", state.board, state.viewFor(ME), 60_000)

    @Test
    fun theSession_followsTheLink() = runTest {
        val session = startedSession()

        assertTrue(connection.started)
        assertEquals(ConnectionState.Connected(ME), session.state.value.connection)
        assertEquals(ME, session.state.value.playerId)

        session.close()
        assertFalse(connection.started)
    }

    @Test
    fun pausingAndResuming_keepsFollowersButReopensTheSocket() = runTest {
        val session = startedSession()

        session.pause()
        assertFalse(connection.started)
        assertEquals(ConnectionState.Offline, session.state.value.connection)

        session.resume()
        assertTrue(connection.started)
    }

    @Test
    fun creatingARoom_waitsForTheServer() = runTest {
        val session = startedSession()
        val settings = RoomSettings(sequenceToWin = 1, firstPlayer = FirstPlayer.GUEST)

        session.createRoom(settings)
        assertTrue(session.state.value.busy)
        runCurrent()
        assertEquals(settings, (connection.sent.single() as ClientMessage.CreateRoom).settings)

        receive(roomState(RoomStatus.WAITING))
        val room = assertNotNull(session.state.value.room)
        assertEquals("K7QP2M", room.code)
        assertEquals(RoomStatus.WAITING, room.status)
        assertFalse(session.state.value.busy)
    }

    @Test
    fun joiningARoom_bringsTheGame() = runTest {
        val session = startedSession()
        val dealt = dealOnlineGame()

        session.joinRoom("  k7qp2m ")
        runCurrent()
        assertEquals("k7qp2m", (connection.sent.single() as ClientMessage.JoinRoom).roomCode)

        receive(roomState(RoomStatus.PLAYING), snapshot(dealt))
        val game = assertNotNull(session.state.value.game)
        assertEquals(dealt.viewFor(ME), game.view)
        assertEquals(dealt.hands.getValue(ME), game.state.hands.getValue(ME))
        assertTrue(game.isMyTurn)
        assertEquals(60.seconds, assertNotNull(game.turnEndsAt) - testScheduler.timeSource.markNow())
    }

    @Test
    fun aMove_isSentOnceAndSettledByTheServer() = runTest {
        val session = startedSession()
        val dealt = dealOnlineGame()
        receive(roomState(RoomStatus.PLAYING), snapshot(dealt))
        val move = GameEngine.legalMoves(dealt, ME).first()

        session.submitMove(move)
        session.submitMove(move)
        runCurrent()
        assertEquals(listOf<ClientMessage>(ClientMessage.SubmitMove("t1", move, 0)), connection.sent)
        assertEquals(true, session.state.value.game?.awaitingMove)

        val played = GameEngine.applyMove(dealt, move).getOrThrow()
        val events = listOf(GameEvent.MovePlayed(ME, move))
        receive(ServerMessage.StateUpdate("u", played.viewFor(ME), events, 60_000))
        val game = assertNotNull(session.state.value.game)
        assertFalse(game.awaitingMove)
        assertFalse(game.isMyTurn)
        assertEquals(events, game.events)
        assertEquals(1, game.view.turnNumber)
    }

    @Test
    fun onTheOpponentsTurn_noMoveIsSent() = runTest {
        val session = startedSession()
        val dealt = dealOnlineGame(first = YOU)
        receive(roomState(RoomStatus.PLAYING), snapshot(dealt))

        session.submitMove(GameEngine.legalMoves(dealt, YOU).first())
        runCurrent()

        assertTrue(connection.sent.isEmpty())
    }

    @Test
    fun refusals_areExplainedInVietnamese() = runTest {
        val session = startedSession()
        val dealt = dealOnlineGame()
        session.joinRoom("ZZZZZZ")
        receive(ServerMessage.Rejected("x", ErrorCodes.ROOM_NOT_FOUND, "No room with code ZZZZZZ", "t1"))
        assertTrue(session.state.value.error!!.startsWith("Không tìm thấy phòng"))
        assertFalse(session.state.value.busy)

        receive(roomState(RoomStatus.PLAYING), snapshot(dealt))
        session.submitMove(GameEngine.legalMoves(dealt, ME).first())
        receive(ServerMessage.Rejected("y", ErrorCodes.ILLEGAL_MOVE, "Position is already occupied", "t2"))
        assertEquals("Ô này đã có quân.", session.state.value.error)
        assertEquals(false, session.state.value.game?.awaitingMove)

        session.clearError()
        assertNull(session.state.value.error)
    }

    @Test
    fun anAwayOpponent_isTrackedWithTheirForfeitClock() = runTest {
        val session = startedSession()
        receive(roomState(RoomStatus.PLAYING), snapshot(dealOnlineGame()))

        receive(ServerMessage.PlayerConnection("a", YOU, connected = false, forfeitInMs = 60_000))
        assertTrue(session.state.value.opponentAway)
        assertEquals(60.seconds, assertNotNull(session.state.value.opponentForfeitAt) - testScheduler.timeSource.markNow())

        receive(ServerMessage.PlayerConnection("b", YOU, connected = true))
        assertFalse(session.state.value.opponentAway)
        assertNull(session.state.value.opponentForfeitAt)

        // News about this player's own link is not about the opponent.
        receive(ServerMessage.PlayerConnection("c", ME, connected = false, forfeitInMs = 60_000))
        assertFalse(session.state.value.opponentAway)
    }

    @Test
    fun theEnd_keepsTheBoardAndStopsTheClock() = runTest {
        val session = startedSession()
        receive(roomState(RoomStatus.PLAYING), snapshot(dealOnlineGame()))
        val result = GameResult(Team.BLUE, GameEndReason.FORFEIT_LEFT)

        receive(ServerMessage.GameOver("o", result), roomState(RoomStatus.FINISHED))

        assertEquals(result, session.state.value.result)
        val game = assertNotNull(session.state.value.game)
        assertNull(game.turnEndsAt)
        assertEquals(RoomStatus.FINISHED, session.state.value.room?.status)

        // A rematch deal starts clean.
        receive(snapshot(dealOnlineGame(first = YOU)))
        assertNull(session.state.value.result)
    }

    @Test
    fun aClosedRoom_isForgotten() = runTest {
        val session = startedSession()
        receive(roomState(RoomStatus.WAITING))

        receive(ServerMessage.RoomClosed("c", "K7QP2M", RoomCloseReason.EXPIRED))

        assertNull(session.state.value.room)
        assertEquals(RoomCloseReason.EXPIRED, session.state.value.roomClosed)
    }

    @Test
    fun aBrandNewSession_dropsTheOldRoom() = runTest {
        val session = startedSession()
        receive(roomState(RoomStatus.PLAYING), snapshot(dealOnlineGame()))

        receive(ServerMessage.Welcome("w", "1.1", "Me", playerId = "someone-new", sessionToken = "fresh"))

        assertEquals("someone-new", session.state.value.playerId)
        assertNull(session.state.value.room)
        assertNull(session.state.value.game)
        assertNotNull(session.state.value.error)
    }

    @Test
    fun aDroppedLink_releasesWaitingRequests() = runTest {
        val session = startedSession()
        session.createRoom(RoomSettings())
        runCurrent()

        connection.state.value = ConnectionState.Reconnecting(attempt = 1, retryInMs = 1_000)
        runCurrent()

        assertFalse(session.state.value.busy)
        assertEquals(ConnectionState.Reconnecting(1, 1_000), session.state.value.connection)
    }

    @Test
    fun withoutALink_requestsFailVisibly() = runTest {
        val session = startedSession()
        connection.online = false

        session.createRoom(RoomSettings())
        runCurrent()

        assertFalse(session.state.value.busy)
        assertNotNull(session.state.value.error)
    }

    @Test
    fun leaving_forgetsTheRoomAtOnce() = runTest {
        val session = startedSession()
        receive(roomState(RoomStatus.PLAYING), snapshot(dealOnlineGame()))

        session.leaveRoom()
        runCurrent()

        assertNull(session.state.value.room)
        assertNull(session.state.value.game)
        assertTrue(connection.sent.single() is ClientMessage.LeaveRoom)
    }

    @Test
    fun theOpponentsRoomState_canShowThemAway() = runTest {
        val session = startedSession()

        receive(roomState(RoomStatus.PLAYING, youConnected = false))

        assertTrue(session.state.value.opponentAway)
    }
}
