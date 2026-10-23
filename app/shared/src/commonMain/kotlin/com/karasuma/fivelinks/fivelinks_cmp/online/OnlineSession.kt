package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEvent
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.PlayerView
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomCloseReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomStatus
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.toGameState
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.localizeMoveError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** Who is in the room and what stage it is at. */
data class OnlineRoom(
    val code: String,
    val settings: RoomSettings,
    val status: RoomStatus,
    val players: List<RoomPlayer>,
)

/** The game as this player sees it. */
data class OnlineGame(
    val board: Board,
    val view: PlayerView,
    /** What happened in the latest update, in order. */
    val events: List<GameEvent> = emptyList(),
    /** When the current turn runs out, on this device's clock; null once the game is over. */
    val turnEndsAt: ComparableTimeMark? = null,
    /** A move was sent and the server has not answered yet. */
    val awaitingMove: Boolean = false,
) {
    /** The view rebuilt into a [GameState], for the shared rules and screens. */
    val state: GameState by lazy { view.toGameState(board) }

    val isMyTurn: Boolean get() = !view.isGameOver && view.currentPlayer.id == view.viewerId
}

data class OnlineState(
    val connection: ConnectionState = ConnectionState.Offline,
    val playerId: PlayerId? = null,
    val room: OnlineRoom? = null,
    val game: OnlineGame? = null,
    val result: GameResult? = null,
    /** The opponent's link is down. */
    val opponentAway: Boolean = false,
    /** When an away opponent forfeits, on this device's clock. */
    val opponentForfeitAt: ComparableTimeMark? = null,
    /** Why the server closed the room this player was in, if it did. */
    val roomClosed: RoomCloseReason? = null,
    /** The last request the server refused, explained in Vietnamese. */
    val error: String? = null,
    /** A room request (create or join) is waiting for the server. */
    val busy: Boolean = false,
)

/**
 * Online play as the screens see it: follows what the server sends and turns the player's
 * actions into requests. The server decides everything; nothing here changes the game on
 * its own.
 */
class OnlineSession(
    private val connection: ServerConnection,
    private val scope: CoroutineScope,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {
    private val _state = MutableStateFlow(OnlineState())
    val state: StateFlow<OnlineState> = _state.asStateFlow()

    private var followers: List<Job> = emptyList()

    /** Connects to the server and starts following it. */
    fun start() {
        if (followers.isNotEmpty()) return
        // Subscribe before connecting so that no message slips by.
        followers = listOf(
            scope.launch(start = CoroutineStart.UNDISPATCHED) { connection.messages.collect(::receive) },
            scope.launch(start = CoroutineStart.UNDISPATCHED) { connection.state.collect(::connectionChanged) },
        )
        connection.start()
    }

    /** Disconnects for good, when the player leaves online play. */
    fun close() {
        connection.stop()
        followers.forEach { it.cancel() }
        followers = emptyList()
    }

    fun createRoom(settings: RoomSettings) {
        _state.update { it.copy(busy = true, error = null, roomClosed = null) }
        send { ClientMessage.CreateRoom(it, settings) }
    }

    fun joinRoom(code: String) {
        _state.update { it.copy(busy = true, error = null, roomClosed = null) }
        send { ClientMessage.JoinRoom(it, code.trim()) }
    }

    /** Plays [move] if it is this player's turn and no other move is on its way. */
    fun submitMove(move: Move) {
        val current = _state.value
        val game = current.game ?: return
        if (!game.isMyTurn || game.awaitingMove || current.result != null) return
        val turn = game.view.turnNumber
        _state.update { it.copy(game = game.copy(awaitingMove = true), error = null) }
        send { ClientMessage.SubmitMove(it, move, turn) }
    }

    fun leaveRoom() {
        _state.update {
            it.copy(room = null, game = null, result = null, opponentAway = false, opponentForfeitAt = null, busy = false)
        }
        send { ClientMessage.LeaveRoom(it) }
    }

    /**
     * Deliberately leaves the current room before closing the socket. This lets the server
     * resolve an in-progress game immediately instead of making the opponent wait through the
     * reconnect grace period.
     */
    suspend fun leaveRoomAndClose() {
        if (_state.value.room != null) {
            connection.send { ClientMessage.LeaveRoom(it) }
        }
        close()
    }

    fun requestRematch() {
        send { ClientMessage.RequestRematch(it) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun send(build: (messageId: String) -> ClientMessage) {
        scope.launch {
            if (!connection.send(build)) {
                _state.update {
                    it.copy(error = NOT_CONNECTED, busy = false, game = it.game?.copy(awaitingMove = false))
                }
            }
        }
    }

    private fun connectionChanged(connection: ConnectionState) {
        _state.update {
            if (connection is ConnectionState.Connected) {
                it.copy(connection = connection)
            } else {
                // Requests in flight are lost with the link; the server resends the truth on reconnect.
                it.copy(connection = connection, busy = false, game = it.game?.copy(awaitingMove = false))
            }
        }
    }

    private fun receive(message: ServerMessage) {
        when (message) {
            is ServerMessage.Welcome -> welcome(message)
            is ServerMessage.RoomState -> _state.update { state ->
                val opponentAway = message.players.any { it.playerId != state.playerId && !it.connected }
                state.copy(
                    room = OnlineRoom(message.roomCode, message.settings, message.status, message.players),
                    roomClosed = null,
                    busy = false,
                    opponentAway = opponentAway,
                    opponentForfeitAt = if (opponentAway) state.opponentForfeitAt else null,
                )
            }
            is ServerMessage.GameSnapshot -> _state.update {
                // A fresh deal or a resync; a finished game is followed by its GameOver.
                it.copy(
                    game = OnlineGame(message.board, message.view, turnEndsAt = deadline(message.view, message.turnTimeLeftMs)),
                    result = null,
                    busy = false,
                )
            }
            is ServerMessage.StateUpdate -> _state.update {
                val game = it.game ?: return@update it
                it.copy(
                    game = game.copy(
                        view = message.view,
                        events = message.events,
                        turnEndsAt = deadline(message.view, message.turnTimeLeftMs),
                        awaitingMove = false,
                    )
                )
            }
            is ServerMessage.PlayerConnection -> _state.update {
                if (message.playerId == it.playerId) return@update it
                it.copy(
                    opponentAway = !message.connected,
                    opponentForfeitAt = message.forfeitInMs?.let { ms -> timeSource.markNow() + ms.milliseconds },
                )
            }
            is ServerMessage.GameOver -> _state.update {
                it.copy(
                    result = message.result,
                    game = it.game?.copy(turnEndsAt = null, awaitingMove = false),
                    opponentForfeitAt = null,
                )
            }
            is ServerMessage.RoomClosed -> _state.update {
                it.copy(room = null, game = null, result = null, roomClosed = message.reason, busy = false)
            }
            is ServerMessage.Rejected -> rejected(message)
            is ServerMessage.Pong -> Unit
        }
    }

    private fun welcome(message: ServerMessage.Welcome) {
        _state.update {
            if (it.playerId != null && it.playerId != message.playerId) {
                // The server started a brand-new session for us: the old room is gone.
                OnlineState(
                    connection = it.connection,
                    playerId = message.playerId,
                    error = if (it.room != null) SESSION_LOST else null,
                )
            } else {
                it.copy(playerId = message.playerId)
            }
        }
    }

    private fun rejected(message: ServerMessage.Rejected) {
        // The client already deals with these two: it starts a new session or reports UpdateRequired.
        if (message.reason == ErrorCodes.SESSION_EXPIRED || message.reason == ErrorCodes.PROTOCOL_VERSION_MISMATCH) return
        _state.update {
            it.copy(error = rejectionText(message), busy = false, game = it.game?.copy(awaitingMove = false))
        }
    }

    private fun deadline(view: PlayerView, timeLeftMs: Long): ComparableTimeMark? =
        if (view.isGameOver) null else timeSource.markNow() + timeLeftMs.milliseconds

    private companion object {
        const val NOT_CONNECTED = "Mất kết nối với máy chủ, đang thử kết nối lại…"
        const val SESSION_LOST = "Phiên chơi cũ đã hết hạn, phòng trước đó không còn."
    }
}

/** What to tell the player when the server refuses a request. */
internal fun rejectionText(rejected: ServerMessage.Rejected): String = when (rejected.reason) {
    ErrorCodes.ROOM_NOT_FOUND -> "Không tìm thấy phòng này. Bạn kiểm tra lại mã phòng nhé."
    ErrorCodes.ROOM_FULL -> "Phòng này đã đủ 2 người."
    ErrorCodes.NOT_IN_ROOM -> "Bạn không còn ở trong phòng."
    ErrorCodes.NOT_YOUR_TURN -> "Chưa đến lượt của bạn."
    ErrorCodes.STALE_TURN -> "Lượt này đã qua."
    ErrorCodes.ILLEGAL_MOVE -> localizeMoveError(rejected.message)
    ErrorCodes.INVALID_REQUEST -> when {
        rejected.message.startsWith("Leave your current room") -> "Bạn đang ở trong một phòng khác."
        rejected.message.startsWith("The game is not over") -> "Ván đấu chưa kết thúc."
        rejected.message.startsWith("Your opponent has left") -> "Đối thủ đã rời phòng."
        rejected.message.startsWith("sequenceToWin") -> "Thiết lập phòng không hợp lệ."
        else -> "Yêu cầu không hợp lệ, bạn thử lại nhé."
    }
    ErrorCodes.PROTOCOL_VERSION_MISMATCH -> "Hãy cập nhật FiveLink lên bản mới nhất để chơi online."
    ErrorCodes.SESSION_EXPIRED -> "Phiên chơi cũ đã hết hạn."
    ErrorCodes.INTERNAL_ERROR -> "Máy chủ đang gặp lỗi, bạn thử lại sau nhé."
}
