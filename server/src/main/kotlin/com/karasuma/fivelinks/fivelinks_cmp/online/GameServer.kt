package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import com.karasuma.fivelinks.fivelinks_cmp.protocol.FirstPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEndReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEvent
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.PassReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolVersion
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomCloseReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomStatus
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.viewFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.random.asKotlinRandom
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Runs every online room. The server keeps the full [GameState]; a player only ever
 * receives their own [com.karasuma.fivelinks.fivelinks_cmp.protocol.PlayerView].
 *
 * Every change happens under one lock, so a move, a timer and a disconnect arriving
 * together are applied one after another. Sending never waits on the network: messages
 * are queued on each [ClientConnection].
 */
class GameServer(
    scope: CoroutineScope,
    val config: OnlineConfig = OnlineConfig(),
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    private val random: Random = SecureRandom().asKotlinRandom(),
) {
    private val log = LoggerFactory.getLogger(GameServer::class.java)
    val metrics = ServerMetrics()
    val generalRateLimiter = RateLimiter(config.rateLimitPerMinute, windowMillis = 60_000L)
    var isShuttingDown: Boolean = false
        private set

    private val timers = CoroutineScope(
        scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]) +
            CoroutineExceptionHandler { _, error -> log.error("Online timer failed", error) }
    )
    private val mutex = Mutex()
    private val messageIds = AtomicLong()
    private val sessions = mutableMapOf<String, PlayerSession>()
    private val rooms = mutableMapOf<String, GameRoom>()

    init {
        timers.launch {
            while (isActive) {
                delay(SWEEP_PERIOD)
                mutex.withLock { sweep() }
            }
        }
    }

    fun activeRoomCount(): Int = rooms.size
    fun activeSessionCount(): Int = sessions.size

    fun status(): ServerStatusPayload = metrics.toStatus(
        activeRooms = rooms.size,
        activeSessions = sessions.size,
        maxRooms = config.maxRooms,
        maxSessions = config.maxSessions,
        isShuttingDown = isShuttingDown,
    )

    fun metrics(): ServerMetricsPayload = metrics.toMetrics(
        activeRooms = rooms.size,
        activeSessions = sessions.size,
        maxRooms = config.maxRooms,
        maxSessions = config.maxSessions,
        isShuttingDown = isShuttingDown,
    )

    /** Handles one message from [connection]. */
    suspend fun handle(connection: ClientConnection, message: ClientMessage) {
        if (config.rateLimitEnabled) {
            val key = connection.session?.token ?: connection.hashCode().toString()
            if (!generalRateLimiter.tryAcquire(key)) {
                metrics.totalRateLimitHits.incrementAndGet()
                log.warn("Rate limit exceeded for key {} on message {}", key, message::class.simpleName)
                connection.send(rejected(ErrorCodes.RATE_LIMITED, "Rate limit exceeded. Please slow down.", message.messageId))
                return
            }
        }

        mutex.withLock {
            try {
                dispatch(connection, message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.error("Failed to handle {}", message::class.simpleName, e)
                connection.send(rejected(ErrorCodes.INTERNAL_ERROR, "Internal error", message.messageId))
            }
        }
    }

    /** A frame that is not a [ClientMessage]. */
    fun unreadable(connection: ClientConnection) {
        connection.send(rejected(ErrorCodes.INVALID_REQUEST, "Unreadable message", inReplyTo = null))
    }

    /** The socket behind [connection] has closed. */
    suspend fun disconnected(connection: ClientConnection) {
        mutex.withLock {
            val session = connection.session ?: return
            connection.session = null
            // Already replaced by a newer socket for the same session.
            if (session.connection !== connection) return
            session.connection = null
            session.lastSeen = timeSource.markNow()
            val room = session.room ?: return
            val seat = room.seatOf(session)
            when (room.status) {
                RoomStatus.PLAYING -> startForfeitClock(room, seat)
                RoomStatus.FINISHED -> notifyConnection(room, seat)
                // The host may come back before anyone joins; the room expires otherwise.
                RoomStatus.WAITING -> Unit
            }
        }
    }

    /** Gracefully closes all rooms and notifies clients when the server stops. */
    suspend fun shutdown() {
        mutex.withLock {
            if (isShuttingDown) return
            isShuttingDown = true
            log.info("Server shutting down, closing {} active rooms and {} sessions", rooms.size, sessions.size)
            for (room in rooms.values.toList()) {
                closeRoom(room, RoomCloseReason.SERVER_SHUTDOWN)
            }
            for (session in sessions.values.toList()) {
                session.connection?.close()
            }
            rooms.clear()
            sessions.clear()
        }
    }

    private fun dispatch(connection: ClientConnection, message: ClientMessage) {
        when (message) {
            is ClientMessage.Ping -> connection.send(ServerMessage.Pong(nextId(), System.currentTimeMillis()))
            is ClientMessage.Hello -> hello(connection, message)
            else -> {
                val session = connection.session
                if (session == null) {
                    connection.send(rejected(ErrorCodes.INVALID_REQUEST, "Say hello first", message.messageId))
                    return
                }
                val request = Request(connection, session, message.messageId)
                when (message) {
                    is ClientMessage.CreateRoom -> createRoom(request, message.settings)
                    is ClientMessage.JoinRoom -> joinRoom(request, message.roomCode)
                    is ClientMessage.SubmitMove -> submitMove(request, message)
                    is ClientMessage.LeaveRoom -> leaveRoom(request)
                    is ClientMessage.RequestRematch -> requestRematch(request)
                    is ClientMessage.Hello, is ClientMessage.Ping -> Unit
                }
            }
        }
    }

    private fun hello(connection: ClientConnection, hello: ClientMessage.Hello) {
        if (!ProtocolVersion.majorCompatible(hello.protocolVersion)) {
            val text = "Server speaks ${ProtocolVersion.STRING}, client ${hello.protocolVersion}"
            connection.send(rejected(ErrorCodes.PROTOCOL_VERSION_MISMATCH, text, hello.messageId))
            connection.close()
            return
        }
        if (isShuttingDown) {
            connection.send(rejected(ErrorCodes.SERVER_BUSY, "Server is shutting down for maintenance", hello.messageId))
            connection.close()
            return
        }
        if (connection.session != null) {
            connection.send(rejected(ErrorCodes.INVALID_REQUEST, "Already said hello", hello.messageId))
            return
        }
        val token = hello.sessionToken
        if (token == null && sessions.size >= config.maxSessions) {
            metrics.totalRateLimitHits.incrementAndGet()
            log.warn("Session capacity reached: {}/{}", sessions.size, config.maxSessions)
            connection.send(rejected(ErrorCodes.SERVER_BUSY, "Server session capacity reached", hello.messageId))
            connection.close()
            return
        }

        val session = if (token == null) {
            newSession(cleanName(hello.playerName))
        } else {
            sessions[token] ?: run {
                val text = "Unknown session, say hello without a token"
                connection.send(rejected(ErrorCodes.SESSION_EXPIRED, text, hello.messageId))
                return
            }
        }
        // The newest socket wins; an older one for the same session is closed.
        session.connection?.takeIf { it !== connection }?.let { old ->
            old.session = null
            old.close()
        }
        session.connection = connection
        connection.session = session
        connection.send(
            ServerMessage.Welcome(nextId(), ProtocolVersion.STRING, session.name, session.playerId, session.token)
        )
        session.room?.let { room ->
            val seat = room.seatOf(session)
            seat.forfeitJob?.cancel()
            seat.forfeitJob = null
            seat.forfeitAt = null
            sync(room, seat)
            notifyConnection(room, seat)
        }
    }

    private fun createRoom(request: Request, settings: RoomSettings) {
        if (isShuttingDown) {
            return request.reject(ErrorCodes.SERVER_BUSY, "Server is shutting down for maintenance")
        }
        if (rooms.size >= config.maxRooms) {
            metrics.totalRateLimitHits.incrementAndGet()
            log.warn("Room capacity reached: {}/{}", rooms.size, config.maxRooms)
            return request.reject(ErrorCodes.SERVER_BUSY, "Server room capacity reached")
        }
        if (settings.sequenceToWin !in 1..2) {
            return request.reject(ErrorCodes.INVALID_REQUEST, "sequenceToWin must be 1 or 2")
        }
        val session = request.session
        if (!leaveFinishedRoom(session)) {
            return request.reject(ErrorCodes.INVALID_REQUEST, "Leave your current room first")
        }
        var code = RoomCodes.generate(random)
        while (code in rooms) code = RoomCodes.generate(random)
        val room = GameRoom(code, settings)
        room.seats += Seat(session, Team.BLUE)
        session.room = room
        rooms[code] = room
        metrics.totalRoomsCreated.incrementAndGet()
        room.expiryJob = timers.launch {
            delay(config.waitingRoomTtl)
            mutex.withLock {
                if (rooms[code] === room && room.status == RoomStatus.WAITING) {
                    closeRoom(room, RoomCloseReason.EXPIRED)
                }
            }
        }
        log.info("Room {} opened by {}", code, session.playerId)
        broadcastRoomState(room)
    }

    private fun joinRoom(request: Request, roomCode: String) {
        val session = request.session
        val code = RoomCodes.normalize(roomCode)
        val current = session.room
        if (current?.code == code) {
            // Already inside (a repeated tap): resend where things stand.
            return sync(current, current.seatOf(session))
        }
        if (!leaveFinishedRoom(session)) {
            return request.reject(ErrorCodes.INVALID_REQUEST, "Leave your current room first")
        }
        val room = rooms[code] ?: return request.reject(ErrorCodes.ROOM_NOT_FOUND, "No room with code $code")
        if (room.status != RoomStatus.WAITING || room.seats.size >= ROOM_SIZE) {
            return request.reject(ErrorCodes.ROOM_FULL, "Room $code is full")
        }
        room.expiryJob?.cancel()
        room.expiryJob = null
        room.seats += Seat(session, Team.RED)
        session.room = room
        log.info("{} joined room {}", session.playerId, code)

        val (host, guest) = room.seats
        val first = when (room.settings.firstPlayer) {
            FirstPlayer.HOST -> host
            FirstPlayer.GUEST -> guest
            FirstPlayer.RANDOM -> if (random.nextBoolean()) host else guest
        }
        startGame(room, first)
    }

    private fun submitMove(request: Request, message: ClientMessage.SubmitMove) {
        val room = request.session.room ?: return request.reject(ErrorCodes.NOT_IN_ROOM, "Not in a room")
        val state = room.game
        if (room.status != RoomStatus.PLAYING || state == null) {
            return request.reject(ErrorCodes.INVALID_REQUEST, "No game in progress")
        }
        val playerId = request.session.playerId
        if (message.turnNumber != state.turnNumber) {
            return request.reject(ErrorCodes.STALE_TURN, "Turn ${message.turnNumber} is over")
        }
        if (state.currentPlayer.id != playerId) {
            return request.reject(ErrorCodes.NOT_YOUR_TURN, "It's not your turn")
        }
        if (message.move.playerId != playerId) {
            return request.reject(ErrorCodes.ILLEGAL_MOVE, "That move belongs to another player")
        }
        val next = GameEngine.applyMove(state, message.move).getOrElse {
            return request.reject(ErrorCodes.ILLEGAL_MOVE, it.message ?: "Illegal move")
        }
        metrics.totalMovesSubmitted.incrementAndGet()
        advance(room, next, listOf(GameEvent.MovePlayed(playerId, message.move)))
    }

    private fun leaveRoom(request: Request) {
        val room = request.session.room ?: return request.reject(ErrorCodes.NOT_IN_ROOM, "Not in a room")
        val seat = room.seatOf(request.session)
        if (room.status == RoomStatus.PLAYING) forfeit(room, seat, GameEndReason.FORFEIT_LEFT)
        removeSeat(room, seat)
    }

    private fun requestRematch(request: Request) {
        val room = request.session.room ?: return request.reject(ErrorCodes.NOT_IN_ROOM, "Not in a room")
        if (room.status != RoomStatus.FINISHED) {
            return request.reject(ErrorCodes.INVALID_REQUEST, "The game is not over")
        }
        if (room.seats.size < ROOM_SIZE) {
            return request.reject(ErrorCodes.INVALID_REQUEST, "Your opponent has left")
        }
        room.seatOf(request.session).wantsRematch = true
        if (room.seats.all { it.wantsRematch }) {
            // Whoever moved second last time starts the rematch.
            startGame(room, room.seats.first { it.session.playerId != room.firstPlayerId })
        } else {
            broadcastRoomState(room)
        }
    }

    private fun startGame(room: GameRoom, first: Seat) {
        val players = (listOf(first) + room.seats.filter { it !== first }).map {
            Player(id = it.session.playerId, name = it.session.name, team = it.team)
        }
        val gameConfig = GameConfig(
            playerCount = players.size,
            teamsByPlayerIndex = players.map { it.team },
            sequenceToWin = room.settings.sequenceToWin,
            seed = random.nextLong(),
            enableTacticalCrafting = room.settings.tacticalCrafting,
        )
        room.game = GameEngine.initialize(gameConfig, players)
        room.gameNumber++
        room.firstPlayerId = first.session.playerId
        room.status = RoomStatus.PLAYING
        room.result = null
        room.seats.forEach { it.wantsRematch = false }
        startTurnClock(room)
        broadcastRoomState(room)
        for (seat in room.seats) seat.session.connection?.send(snapshot(room, seat))
        // Someone who dropped while the room was waiting starts the game away.
        room.seats.filterNot { it.connected }.forEach { startForfeitClock(room, it) }
        log.info("Room {} game {} started, {} moves first", room.code, room.gameNumber, first.session.playerId)
    }

    /** Applies the turn rules after a move or a pass, then tells both players. */
    private fun advance(room: GameRoom, next: GameState, events: List<GameEvent>) {
        val skipped = GameEngine.skipStuckTurns(next)
        val state = skipped.state
        room.game = state
        val allEvents = events + skipped.passed.map { GameEvent.TurnPassed(it, PassReason.NO_LEGAL_MOVES) }
        if (!state.isGameOver) startTurnClock(room)
        for (seat in room.seats) {
            val view = state.viewFor(seat.session.playerId)
            val timeLeft = if (state.isGameOver) 0 else turnTimeLeftMs(room)
            seat.session.connection?.send(ServerMessage.StateUpdate(nextId(), view, allEvents, timeLeft))
        }
        if (state.isGameOver) {
            val reason = if (state.winner != null) GameEndReason.COMPLETED_SEQUENCES else GameEndReason.DRAW_NO_MOVES
            finishGame(room, GameResult(state.winner, reason))
        }
    }

    private fun startTurnClock(room: GameRoom) {
        room.turnJob?.cancel()
        val gameNumber = room.gameNumber
        val turn = room.game?.turnNumber ?: return
        room.turnEndsAt = timeSource.markNow() + config.turnTime
        room.turnJob = timers.launch {
            delay(config.turnTime)
            mutex.withLock {
                val state = room.game
                if (state != null && room.status == RoomStatus.PLAYING && room.gameNumber == gameNumber &&
                    state.turnNumber == turn
                ) {
                    val passed = GameEvent.TurnPassed(state.currentPlayer.id, PassReason.TIMEOUT)
                    advance(room, GameEngine.advanceTurn(state), listOf(passed))
                }
            }
        }
    }

    private fun startForfeitClock(room: GameRoom, seat: Seat) {
        seat.forfeitJob?.cancel()
        val gameNumber = room.gameNumber
        seat.forfeitAt = timeSource.markNow() + config.reconnectGrace
        seat.forfeitJob = timers.launch {
            delay(config.reconnectGrace)
            mutex.withLock {
                if (room.status == RoomStatus.PLAYING && room.gameNumber == gameNumber &&
                    !seat.connected && seat in room.seats
                ) {
                    forfeit(room, seat, GameEndReason.FORFEIT_DISCONNECTED)
                }
            }
        }
        notifyConnection(room, seat)
    }

    private fun forfeit(room: GameRoom, loser: Seat, reason: GameEndReason) {
        val winner = room.seats.firstOrNull { it !== loser }?.team
        // Record the result on the game too, so every view of it reads as over.
        room.game = room.game?.copy(winner = winner)
        finishGame(room, GameResult(winner, reason))
    }

    private fun finishGame(room: GameRoom, result: GameResult) {
        metrics.totalGamesFinished.incrementAndGet()
        room.turnJob?.cancel()
        room.turnJob = null
        room.turnEndsAt = null
        for (seat in room.seats) {
            seat.forfeitJob?.cancel()
            seat.forfeitJob = null
            seat.forfeitAt = null
        }
        room.status = RoomStatus.FINISHED
        room.result = result
        for (seat in room.seats) seat.session.connection?.send(ServerMessage.GameOver(nextId(), result))
        broadcastRoomState(room)
        log.info("Room {} game {} over: {} ({})", room.code, room.gameNumber, result.winner ?: "draw", result.reason)
    }

    /** Frees [session] for another room: leaving a finished room is implied, an active one is not. */
    private fun leaveFinishedRoom(session: PlayerSession): Boolean {
        val room = session.room ?: return true
        if (room.status != RoomStatus.FINISHED) return false
        removeSeat(room, room.seatOf(session))
        return true
    }

    private fun removeSeat(room: GameRoom, seat: Seat) {
        seat.forfeitJob?.cancel()
        room.seats.remove(seat)
        seat.session.room = null
        if (room.seats.isEmpty()) removeRoom(room) else broadcastRoomState(room)
    }

    private fun closeRoom(room: GameRoom, reason: RoomCloseReason) {
        for (seat in room.seats) {
            seat.session.connection?.send(ServerMessage.RoomClosed(nextId(), room.code, reason))
        }
        log.info("Room {} closed: {}", room.code, reason)
        removeRoom(room)
    }

    private fun removeRoom(room: GameRoom) {
        room.turnJob?.cancel()
        room.expiryJob?.cancel()
        for (seat in room.seats) {
            seat.forfeitJob?.cancel()
            seat.session.room = null
        }
        room.seats.clear()
        rooms.remove(room.code)
    }

    /** Forgets finished rooms everyone walked away from, and sessions nobody uses any more. */
    private fun sweep() {
        val now = timeSource.markNow()
        rooms.values
            .filter { room ->
                room.status == RoomStatus.FINISHED && room.seats.none { it.connected } &&
                    room.seats.all { it.session.lastSeen + config.reconnectGrace <= now }
            }
            .forEach(::removeRoom)
        sessions.values.removeAll {
            it.connection == null && it.room == null && it.lastSeen + config.idleSessionTtl <= now
        }
        if (config.rateLimitEnabled) {
            generalRateLimiter.cleanup()
        }
    }

    /** Sends [seat]'s player everything they need to show the room as it is now. */
    private fun sync(room: GameRoom, seat: Seat) {
        val connection = seat.session.connection ?: return
        connection.send(roomState(room))
        if (room.status == RoomStatus.WAITING) return
        connection.send(snapshot(room, seat))
        room.result?.let { connection.send(ServerMessage.GameOver(nextId(), it)) }
        for (other in room.seats) {
            if (other !== seat && !other.connected) connection.send(connectionNotice(other))
        }
    }

    /** Tells the others in [room] that [seat]'s player dropped or came back. */
    private fun notifyConnection(room: GameRoom, seat: Seat) {
        for (other in room.seats) {
            if (other !== seat) other.session.connection?.send(connectionNotice(seat))
        }
    }

    private fun connectionNotice(seat: Seat) = ServerMessage.PlayerConnection(
        nextId(),
        seat.session.playerId,
        seat.connected,
        forfeitInMs = if (seat.connected) null else seat.forfeitAt?.let(::millisUntil),
    )

    private fun broadcastRoomState(room: GameRoom) {
        for (seat in room.seats) seat.session.connection?.send(roomState(room))
    }

    private fun roomState(room: GameRoom) = ServerMessage.RoomState(
        nextId(),
        room.code,
        room.settings,
        room.status,
        room.seats.map { RoomPlayer(it.session.playerId, it.session.name, it.team, it.connected, it.wantsRematch) },
    )

    private fun snapshot(room: GameRoom, seat: Seat): ServerMessage.GameSnapshot {
        val state = checkNotNull(room.game)
        val view = state.viewFor(seat.session.playerId)
        return ServerMessage.GameSnapshot(nextId(), room.code, state.board, view, turnTimeLeftMs(room))
    }

    private fun turnTimeLeftMs(room: GameRoom): Long = room.turnEndsAt?.let(::millisUntil) ?: 0

    private fun millisUntil(mark: ComparableTimeMark): Long =
        (mark - timeSource.markNow()).inWholeMilliseconds.coerceAtLeast(0)

    private fun newSession(name: String): PlayerSession {
        val session = PlayerSession(
            playerId = "p_" + randomString(8),
            token = randomString(32),
            name = name,
            lastSeen = timeSource.markNow(),
        )
        sessions[session.token] = session
        return session
    }

    private fun randomString(length: Int): String = buildString(length) {
        repeat(length) { append(ID_ALPHABET[random.nextInt(ID_ALPHABET.length)]) }
    }

    private fun cleanName(raw: String): String = raw
        .filterNot { it.isISOControl() }
        .trim()
        .replace(WHITESPACE, " ")
        .take(MAX_NAME_LENGTH)
        .trim()
        .ifEmpty { DEFAULT_NAME }

    private fun rejected(reason: ErrorCodes, text: String, inReplyTo: String?) =
        ServerMessage.Rejected(nextId(), reason, text, inReplyTo)

    private fun nextId(): String = "s${messageIds.incrementAndGet()}"

    private inner class Request(
        val connection: ClientConnection,
        val session: PlayerSession,
        val messageId: String,
    ) {
        fun reject(reason: ErrorCodes, text: String) = connection.send(rejected(reason, text, messageId))
    }

    private companion object {
        const val ROOM_SIZE = 2
        const val MAX_NAME_LENGTH = 20
        const val DEFAULT_NAME = "Người chơi"
        const val ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
        val SWEEP_PERIOD = 30.seconds
        val WHITESPACE = Regex("\\s+")
    }
}
