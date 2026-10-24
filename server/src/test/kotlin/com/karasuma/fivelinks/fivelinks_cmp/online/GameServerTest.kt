package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import com.karasuma.fivelinks.fivelinks_cmp.protocol.FirstPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEndReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEvent
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.PassReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomCloseReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomStatus
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.toGameState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class) // the test scheduler's time source
class GameServerTest {

    private fun TestScope.newServer() =
        GameServer(backgroundScope, OnlineConfig(), testScheduler.timeSource, Random(7))

    private fun TestScope.wait(duration: Duration) {
        advanceTimeBy(duration)
        runCurrent()
    }

    /** The host opens a room and the guest joins it, which starts the game. */
    private suspend fun startedGame(
        server: GameServer,
        settings: RoomSettings = RoomSettings(firstPlayer = FirstPlayer.HOST),
    ): Pair<FakeClient, FakeClient> {
        val host = FakeClient(server).hello("Host")
        host.send(ClientMessage.CreateRoom(host.nextId(), settings))
        val code = host.drain().only<ServerMessage.RoomState>().roomCode
        val guest = FakeClient(server).hello("Guest")
        guest.send(ClientMessage.JoinRoom(guest.nextId(), code))
        return host to guest
    }

    @Test
    fun hello_opensASession() = runTest {
        val client = FakeClient(newServer())
        client.send(ClientMessage.Hello("1", playerName = "  An \n  Nguyen "))

        val welcome = client.drain().only<ServerMessage.Welcome>()
        assertEquals("An Nguyen", welcome.playerName)
        assertTrue(welcome.playerId.isNotBlank())
        assertTrue(welcome.sessionToken.length >= 32)
    }

    @Test
    fun anIncompatibleApp_isTurnedAway() = runTest {
        val client = FakeClient(newServer())
        client.send(ClientMessage.Hello("1", protocolVersion = "2.0", playerName = "An"))

        assertEquals(ErrorCodes.PROTOCOL_VERSION_MISMATCH, client.drain().only<ServerMessage.Rejected>().reason)
        assertTrue(client.connection.outgoing.tryReceive().isClosed)
    }

    @Test
    fun roomRequests_needAHelloFirst() = runTest {
        val client = FakeClient(newServer())
        client.send(ClientMessage.CreateRoom("1"))

        val rejected = client.drain().only<ServerMessage.Rejected>()
        assertEquals(ErrorCodes.INVALID_REQUEST, rejected.reason)
        assertEquals("1", rejected.inReplyTo)
    }

    @Test
    fun anUnknownToken_meansStartingOver() = runTest {
        val client = FakeClient(newServer())
        client.send(ClientMessage.Hello("1", playerName = "An", sessionToken = "forgotten"))
        assertEquals(ErrorCodes.SESSION_EXPIRED, client.drain().only<ServerMessage.Rejected>().reason)

        client.hello("An")
        assertTrue(client.playerId.isNotBlank())
    }

    @Test
    fun aNewRoom_waitsWithAReadableCode() = runTest {
        val host = FakeClient(newServer()).hello()
        host.send(ClientMessage.CreateRoom(host.nextId()))

        val room = host.drain().only<ServerMessage.RoomState>()
        assertEquals(RoomStatus.WAITING, room.status)
        assertEquals(6, room.roomCode.length)
        assertTrue(room.roomCode.all { it in RoomCodes.ALPHABET })
        assertEquals(listOf(host.playerId), room.players.map { it.playerId })
        assertEquals(Team.BLUE, room.players.single().team)
    }

    @Test
    fun joining_dealsEachPlayerOnlyTheirOwnHand() = runTest {
        val (host, guest) = startedGame(newServer())

        val hands = listOf(host, guest).map { client ->
            val messages = client.drain()
            val room = messages.lastOf<ServerMessage.RoomState>()
            assertEquals(RoomStatus.PLAYING, room.status)
            assertEquals(listOf(host.playerId, guest.playerId), room.players.map { it.playerId })

            val snapshot = messages.only<ServerMessage.GameSnapshot>()
            assertEquals(client.playerId, snapshot.view.viewerId)
            assertEquals(7, snapshot.view.hand.size)
            assertEquals(mapOf(host.playerId to 7, guest.playerId to 7), snapshot.view.handSizes)
            assertEquals(0L, snapshot.view.config.seed)
            assertEquals(60_000L, snapshot.turnTimeLeftMs)
            snapshot.view.hand
        }
        assertNotEquals(hands[0], hands[1])
    }

    @Test
    fun roomCodes_ignoreCaseSpacesAndDashes() = runTest {
        val server = newServer()
        val host = FakeClient(server).hello()
        host.send(ClientMessage.CreateRoom(host.nextId()))
        val code = host.drain().only<ServerMessage.RoomState>().roomCode

        val guest = FakeClient(server).hello()
        guest.send(ClientMessage.JoinRoom(guest.nextId(), " " + code.lowercase().chunked(3).joinToString("-")))
        assertEquals(RoomStatus.PLAYING, guest.drain().lastOf<ServerMessage.RoomState>().status)
    }

    @Test
    fun joinMistakes_areExplained() = runTest {
        val server = newServer()
        val (host, _) = startedGame(server)
        val code = host.drain().lastOf<ServerMessage.RoomState>().roomCode
        val third = FakeClient(server).hello("Third")

        third.send(ClientMessage.JoinRoom(third.nextId(), "ZZZZZZ"))
        assertEquals(ErrorCodes.ROOM_NOT_FOUND, third.drain().only<ServerMessage.Rejected>().reason)

        third.send(ClientMessage.JoinRoom(third.nextId(), code))
        assertEquals(ErrorCodes.ROOM_FULL, third.drain().only<ServerMessage.Rejected>().reason)
    }

    @Test
    fun theCreator_choosesWhoStarts() = runTest {
        val server = newServer()

        val (host, _) = startedGame(server, RoomSettings(firstPlayer = FirstPlayer.HOST))
        assertEquals(host.playerId, host.drain().only<ServerMessage.GameSnapshot>().view.currentPlayer.id)

        val (_, guest) = startedGame(server, RoomSettings(firstPlayer = FirstPlayer.GUEST))
        assertEquals(guest.playerId, guest.drain().only<ServerMessage.GameSnapshot>().view.currentPlayer.id)
    }

    @Test
    fun roomSettings_becomeTheGameRules() = runTest {
        val (host, _) = startedGame(newServer(), RoomSettings(sequenceToWin = 1, tacticalCrafting = false))

        val config = host.drain().only<ServerMessage.GameSnapshot>().view.config
        assertEquals(1, config.sequenceToWin)
        assertFalse(config.enableTacticalCrafting)
    }

    @Test
    fun outOfRangeSettings_areRefused() = runTest {
        val host = FakeClient(newServer()).hello()
        host.send(ClientMessage.CreateRoom(host.nextId(), RoomSettings(sequenceToWin = 5)))

        assertEquals(ErrorCodes.INVALID_REQUEST, host.drain().only<ServerMessage.Rejected>().reason)
    }

    @Test
    fun moves_areCheckedByTheServer() = runTest {
        val (host, guest) = startedGame(newServer())
        val legal = host.legalMove()
        guest.drain()

        // The guest tries to play on the host's turn.
        val guestCard = guest.view.hand.cards.first()
        guest.send(ClientMessage.SubmitMove(guest.nextId(), Move.Place(guest.playerId, guestCard, BoardPosition(0, 1)), 0))
        assertEquals(ErrorCodes.NOT_YOUR_TURN, guest.drain().only<ServerMessage.Rejected>().reason)

        // A move for a turn that is not the current one.
        host.send(legal.copy(messageId = host.nextId(), turnNumber = 5))
        assertEquals(ErrorCodes.STALE_TURN, host.drain().only<ServerMessage.Rejected>().reason)

        // A move made in the opponent's name.
        val forged = Move.Place(guest.playerId, guestCard, BoardPosition(0, 1))
        host.send(ClientMessage.SubmitMove(host.nextId(), forged, 0))
        assertEquals(ErrorCodes.ILLEGAL_MOVE, host.drain().only<ServerMessage.Rejected>().reason)

        // A card placed on a cell that shows a different card.
        val card = host.view.hand.cards.first { !it.isJack() }
        val wrongCell = BoardPosition.all.first { !host.board.isCorner(it) && host.board.cardAt(it) != card }
        host.send(ClientMessage.SubmitMove(host.nextId(), Move.Place(host.playerId, card, wrongCell), 0))
        assertEquals(ErrorCodes.ILLEGAL_MOVE, host.drain().only<ServerMessage.Rejected>().reason)

        // Nothing above changed the game; a legal move goes through for both players.
        host.send(legal.copy(messageId = host.nextId()))
        for (client in listOf(host, guest)) {
            val update = client.drain().only<ServerMessage.StateUpdate>()
            assertEquals(listOf(GameEvent.MovePlayed(host.playerId, legal.move)), update.events)
            assertEquals(1, update.view.turnNumber)
            assertEquals(guest.playerId, update.view.currentPlayer.id)
            assertEquals(client.playerId, update.view.viewerId)
        }
        assertEquals(7, host.view.hand.size)
    }

    @Test
    fun completingASequence_endsTheGameForBoth() = runTest {
        val (host, guest) = startedGame(newServer(), RoomSettings(sequenceToWin = 1, firstPlayer = FirstPlayer.HOST))
        val players = listOf(host, guest)
        val ai = HeuristicEvaluator(random = Random(3))

        var turns = 0
        while (host.inbox.none { it is ServerMessage.GameOver } && turns++ < 200) {
            players.forEach { it.drain() }
            if (host.view.isGameOver) continue
            val mover = players.single { it.playerId == host.view.currentPlayer.id }
            val view = mover.view
            val move = ai.chooseMove(view.toGameState(mover.board), mover.playerId, Difficulty.HARD)
            mover.send(ClientMessage.SubmitMove(mover.nextId(), move, view.turnNumber))
            players.forEach { it.drain() }
        }

        val result = host.inbox.only<ServerMessage.GameOver>().result
        assertEquals(GameEndReason.COMPLETED_SEQUENCES, result.reason)
        val winner = assertNotNull(result.winner)
        assertEquals(result, guest.inbox.only<ServerMessage.GameOver>().result)
        assertTrue(host.view.completedSequence.any { it.team == winner })
        assertEquals(RoomStatus.FINISHED, guest.inbox.lastOf<ServerMessage.RoomState>().status)
    }

    @Test
    fun aSlowPlayer_passesWhenTheTurnClockRunsOut() = runTest {
        val (host, guest) = startedGame(newServer())
        guest.drain()

        wait(59.seconds)
        assertTrue(guest.drain().isEmpty())

        wait(1.seconds)
        val update = guest.drain().only<ServerMessage.StateUpdate>()
        assertEquals(listOf(GameEvent.TurnPassed(host.playerId, PassReason.TIMEOUT)), update.events)
        assertEquals(guest.playerId, update.view.currentPlayer.id)
        assertNull(update.view.lastMove)
        assertEquals(60_000L, update.turnTimeLeftMs)
    }

    @Test
    fun aMove_restartsTheTurnClock() = runTest {
        val (host, guest) = startedGame(newServer())
        wait(30.seconds)
        host.send(host.legalMove())

        wait(59.seconds)
        assertTrue(host.drain().filterIsInstance<ServerMessage.StateUpdate>().none { update ->
            update.events.any { it is GameEvent.TurnPassed }
        })

        wait(1.seconds)
        assertEquals(
            listOf(GameEvent.TurnPassed(guest.playerId, PassReason.TIMEOUT)),
            host.drain().only<ServerMessage.StateUpdate>().events,
        )
    }

    @Test
    fun stayingAwayTooLong_forfeitsTheGame() = runTest {
        val (host, guest) = startedGame(newServer())
        wait(10.seconds)
        guest.drain()

        host.drop()
        val away = guest.drain().only<ServerMessage.PlayerConnection>()
        assertEquals(host.playerId, away.playerId)
        assertFalse(away.connected)
        assertEquals(60_000L, away.forfeitInMs)

        wait(59.seconds)
        assertTrue(guest.drain().none { it is ServerMessage.GameOver })

        wait(1.seconds)
        val messages = guest.drain()
        assertEquals(GameResult(Team.RED, GameEndReason.FORFEIT_DISCONNECTED), messages.only<ServerMessage.GameOver>().result)
        assertEquals(RoomStatus.FINISHED, messages.lastOf<ServerMessage.RoomState>().status)
    }

    @Test
    fun comingBackInTime_resumesTheGame() = runTest {
        val (host, guest) = startedGame(newServer())
        host.drop()
        wait(30.seconds)
        guest.drain()

        host.reconnect()
        val messages = host.drain()
        assertEquals(host.playerId, messages.only<ServerMessage.Welcome>().playerId)
        assertEquals(RoomStatus.PLAYING, messages.only<ServerMessage.RoomState>().status)
        val snapshot = messages.only<ServerMessage.GameSnapshot>()
        assertEquals(host.playerId, snapshot.view.viewerId)
        assertEquals(30_000L, snapshot.turnTimeLeftMs)

        val back = guest.drain().only<ServerMessage.PlayerConnection>()
        assertTrue(back.connected)
        assertNull(back.forfeitInMs)

        wait(5.minutes)
        assertTrue(host.drain().none { it is ServerMessage.GameOver })
    }

    @Test
    fun aSecondSocket_takesOverTheSession() = runTest {
        val client = FakeClient(newServer()).hello()
        val first = client.connection

        client.reconnect()
        assertEquals(client.playerId, client.drain().only<ServerMessage.Welcome>().playerId)
        assertTrue(first.outgoing.tryReceive().isClosed)
    }

    @Test
    fun leavingMidGame_forfeits() = runTest {
        val (host, guest) = startedGame(newServer())
        host.drain()

        guest.send(ClientMessage.LeaveRoom(guest.nextId()))
        val messages = host.drain()
        assertEquals(GameResult(Team.BLUE, GameEndReason.FORFEIT_LEFT), messages.only<ServerMessage.GameOver>().result)
        val room = messages.lastOf<ServerMessage.RoomState>()
        assertEquals(RoomStatus.FINISHED, room.status)
        assertEquals(listOf(host.playerId), room.players.map { it.playerId })

        host.send(ClientMessage.RequestRematch(host.nextId()))
        assertEquals(ErrorCodes.INVALID_REQUEST, host.drain().only<ServerMessage.Rejected>().reason)
    }

    @Test
    fun aRematch_letsTheOtherPlayerStart() = runTest {
        val (host, guest) = startedGame(newServer(), RoomSettings(firstPlayer = FirstPlayer.HOST))
        // End the first game: the guest drops and misses the grace period.
        wait(1.seconds)
        guest.drop()
        wait(60.seconds)
        guest.reconnect()
        assertEquals(GameEndReason.FORFEIT_DISCONNECTED, guest.drain().only<ServerMessage.GameOver>().result.reason)
        host.drain()

        host.send(ClientMessage.RequestRematch(host.nextId()))
        val waiting = guest.drain().only<ServerMessage.RoomState>()
        assertTrue(waiting.players.single { it.playerId == host.playerId }.wantsRematch)
        assertEquals(RoomStatus.FINISHED, waiting.status)

        guest.send(ClientMessage.RequestRematch(guest.nextId()))
        val snapshot = host.drain().only<ServerMessage.GameSnapshot>()
        assertEquals(guest.playerId, snapshot.view.currentPlayer.id)
        assertEquals(0, snapshot.view.turnNumber)
        assertEquals(7, snapshot.view.hand.size)
        assertEquals(RoomStatus.PLAYING, host.inbox.lastOf<ServerMessage.RoomState>().status)
    }

    @Test
    fun anUnjoinedRoom_closesAfterTenMinutes() = runTest {
        val server = newServer()
        val host = FakeClient(server).hello()
        host.send(ClientMessage.CreateRoom(host.nextId()))
        val code = host.drain().only<ServerMessage.RoomState>().roomCode

        wait(10.minutes)
        val closed = host.drain().only<ServerMessage.RoomClosed>()
        assertEquals(code, closed.roomCode)
        assertEquals(RoomCloseReason.EXPIRED, closed.reason)

        val late = FakeClient(server).hello()
        late.send(ClientMessage.JoinRoom(late.nextId(), code))
        assertEquals(ErrorCodes.ROOM_NOT_FOUND, late.drain().only<ServerMessage.Rejected>().reason)

        // The host is free to open another room.
        host.send(ClientMessage.CreateRoom(host.nextId()))
        assertEquals(RoomStatus.WAITING, host.drain().only<ServerMessage.RoomState>().status)
    }

    @Test
    fun ping_isAnsweredBeforeHello() = runTest {
        val client = FakeClient(newServer())
        client.send(ClientMessage.Ping("1", clientTime = 1))
        client.drain().only<ServerMessage.Pong>()
    }

    @Test
    fun roomCapacity_reached_rejectsCreateRoom() = runTest {
        val config = OnlineConfig(maxRooms = 1)
        val server = GameServer(backgroundScope, config, testScheduler.timeSource, Random(7))
        val host1 = FakeClient(server).hello("Host1")
        host1.send(ClientMessage.CreateRoom(host1.nextId()))
        assertEquals(RoomStatus.WAITING, host1.drain().only<ServerMessage.RoomState>().status)

        val host2 = FakeClient(server).hello("Host2")
        host2.send(ClientMessage.CreateRoom(host2.nextId()))
        val rejected = host2.drain().only<ServerMessage.Rejected>()
        assertEquals(ErrorCodes.SERVER_BUSY, rejected.reason)
    }

    @Test
    fun sessionCapacity_reached_rejectsHello() = runTest {
        val config = OnlineConfig(maxSessions = 1)
        val server = GameServer(backgroundScope, config, testScheduler.timeSource, Random(7))
        val client1 = FakeClient(server)
        client1.hello("Client1")

        val client2 = FakeClient(server)
        client2.send(ClientMessage.Hello(client2.nextId(), playerName = "Client2"))
        val rejected = client2.drain().only<ServerMessage.Rejected>()
        assertEquals(ErrorCodes.SERVER_BUSY, rejected.reason)
    }

    @Test
    fun rateLimiter_flood_rejectsWithRateLimited() = runTest {
        val config = OnlineConfig(rateLimitPerMinute = 2, rateLimitEnabled = true)
        val server = GameServer(backgroundScope, config, testScheduler.timeSource, Random(7))
        val client = FakeClient(server)
        // 1st request
        client.send(ClientMessage.Ping(client.nextId(), clientTime = 1))
        client.drain().only<ServerMessage.Pong>()
        // 2nd request
        client.send(ClientMessage.Ping(client.nextId(), clientTime = 2))
        client.drain().only<ServerMessage.Pong>()
        // 3rd request (exceeds limit 2)
        client.send(ClientMessage.Ping(client.nextId(), clientTime = 3))
        val rejected = client.drain().only<ServerMessage.Rejected>()
        assertEquals(ErrorCodes.RATE_LIMITED, rejected.reason)
    }

    @Test
    fun serverShutdown_notifiesActiveRooms() = runTest {
        val server = newServer()
        val (host, guest) = startedGame(server)
        host.drain()
        guest.drain()

        server.shutdown()
        val hostClosed = host.drain().only<ServerMessage.RoomClosed>()
        val guestClosed = guest.drain().only<ServerMessage.RoomClosed>()
        assertEquals(RoomCloseReason.SERVER_SHUTDOWN, hostClosed.reason)
        assertEquals(RoomCloseReason.SERVER_SHUTDOWN, guestClosed.reason)
    }
}
