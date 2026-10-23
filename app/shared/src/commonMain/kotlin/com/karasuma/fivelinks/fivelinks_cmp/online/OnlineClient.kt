package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Where the game server listens. While developing, a phone on USB reaches the server on
 * the laptop through `adb reverse tcp:8080 tcp:8080`.
 */
object ServerConfig {
    const val DEV_URL = "ws://localhost:8080/ws"

    /** Returns a Vietnamese setup error instead of letting a bad release configuration retry forever. */
    fun validationError(url: String, requireSecure: Boolean = false): String? {
        val normalized = url.trim()
        return when {
            normalized.isEmpty() -> "Bản này chưa được cấu hình máy chủ online."
            requireSecure && !normalized.startsWith("wss://") ->
                "Bản phát hành chỉ chấp nhận máy chủ bảo mật wss://."
            !normalized.startsWith("ws://") && !normalized.startsWith("wss://") ->
                "Địa chỉ máy chủ phải bắt đầu bằng ws:// hoặc wss://."
            !normalized.endsWith("/ws") -> "Địa chỉ máy chủ phải kết thúc bằng /ws."
            else -> null
        }
    }
}

/** The link to the game server, as the screens see it. */
sealed interface ConnectionState {
    /** Not started yet, or stopped on purpose. */
    data object Offline : ConnectionState

    data object Connecting : ConnectionState

    data class Connected(val playerId: PlayerId) : ConnectionState

    /** The link dropped or could not be made; the next try starts in [retryInMs]. */
    data class Reconnecting(val attempt: Int, val retryInMs: Long) : ConnectionState

    /** The server speaks a newer protocol: this app needs an update. */
    data object UpdateRequired : ConnectionState
}

/** A link to the game server. */
interface ServerConnection {
    val state: StateFlow<ConnectionState>

    /** What the server sends, in order, apart from heartbeat replies. */
    val messages: Flow<ServerMessage>

    fun start()

    fun stop()

    /** Sends the message [build] makes for a fresh message id; false when there is no live link. */
    suspend fun send(build: (messageId: String) -> ClientMessage): Boolean
}

/**
 * The app's WebSocket to the game server. It says hello, keeps the link alive with pings
 * and, after a drop, reconnects on its own with the session token, so the server puts the
 * player back in their room.
 *
 * Call it from one thread (the main thread in the app).
 */
class OnlineClient(
    private val playerName: String,
    private val scope: CoroutineScope,
    private val url: String = ServerConfig.DEV_URL,
    private val httpClient: HttpClient = createGameHttpClient(),
    initialSessionToken: String? = null,
    private val onSessionTokenChanged: (String?) -> Unit = {},
) : ServerConnection {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Offline)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val messages: Flow<ServerMessage> = _messages.asSharedFlow()

    /** Lets the server resume this player's session, and their room, after a drop. */
    var sessionToken: String? = initialSessionToken
        private set

    private var socket: DefaultClientWebSocketSession? = null
    private var loop: Job? = null
    private var failures = 0
    private var lastMessageId = 0
    private val clock = TimeSource.Monotonic.markNow()

    override fun start() {
        if (loop?.isActive == true) return
        loop = scope.launch { connectUntilStopped() }
    }

    override fun stop() {
        loop?.cancel()
        loop = null
        socket = null
        _state.value = ConnectionState.Offline
    }

    override suspend fun send(build: (messageId: String) -> ClientMessage): Boolean {
        val socket = socket ?: return false
        if (_state.value !is ConnectionState.Connected) return false
        return socket.sendMessage(build(nextMessageId()))
    }

    /** Cuts the current link the way a network failure would; the client then reconnects. */
    internal suspend fun dropConnection() {
        socket?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Dropped on purpose"))
    }

    private suspend fun connectUntilStopped() {
        failures = 0
        while (true) {
            if (failures == 0) _state.value = ConnectionState.Connecting
            val updateRequired = try {
                connectOnce()
            } catch (e: CancellationException) {
                // Leave if stop() was called; otherwise only the socket was cancelled.
                currentCoroutineContext().ensureActive()
                false
            } catch (e: Exception) {
                false
            }
            if (updateRequired) {
                _state.value = ConnectionState.UpdateRequired
                return
            }
            failures++
            val wait = retryDelay(failures)
            _state.value = ConnectionState.Reconnecting(failures, wait.inWholeMilliseconds)
            delay(wait)
        }
    }

    /** Runs one WebSocket session to its end; true when the server turned this app version away. */
    private suspend fun connectOnce(): Boolean {
        var updateRequired = false
        httpClient.webSocket(url) {
            socket = this
            val session = this
            val heartbeat = scope.launch {
                while (true) {
                    delay(PING_EVERY)
                    session.sendMessage(ClientMessage.Ping(nextMessageId(), clock.elapsedNow().inWholeMilliseconds))
                }
            }
            try {
                sendMessage(ClientMessage.Hello(nextMessageId(), playerName = playerName, sessionToken = sessionToken))
                while (true) {
                    // The server answers every ping, so a long silence means the link is dead.
                    val frame = withTimeoutOrNull(SILENCE_LIMIT) { incoming.receiveCatching() }?.getOrNull() ?: break
                    val message = (frame as? Frame.Text)?.let { decode(it.readText()) } ?: continue
                    when (message) {
                        is ServerMessage.Welcome -> {
                            updateSessionToken(message.sessionToken)
                            failures = 0
                            _state.value = ConnectionState.Connected(message.playerId)
                        }
                        is ServerMessage.Rejected -> when (message.reason) {
                            // The server no longer knows this session (it restarted, or we were
                            // away too long): start a fresh one.
                            ErrorCodes.SESSION_EXPIRED -> {
                                updateSessionToken(null)
                                sendMessage(ClientMessage.Hello(nextMessageId(), playerName = playerName))
                            }
                            ErrorCodes.PROTOCOL_VERSION_MISMATCH -> updateRequired = true
                            else -> Unit
                        }
                        else -> Unit
                    }
                    if (message !is ServerMessage.Pong) _messages.emit(message)
                }
            } finally {
                heartbeat.cancel()
                if (socket === session) socket = null
            }
        }
        return updateRequired
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: ClientMessage): Boolean {
        val text = ProtocolJson.encodeToString(ClientMessage.serializer(), message)
        return withTimeoutOrNull(SEND_TIMEOUT) {
            try {
                send(Frame.Text(text))
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    /** Null for anything this app does not understand, such as a message type added later. */
    private fun decode(text: String): ServerMessage? =
        runCatching { ProtocolJson.decodeFromString(ServerMessage.serializer(), text) }.getOrNull()

    private fun nextMessageId(): String = "c${++lastMessageId}"

    private fun updateSessionToken(value: String?) {
        if (sessionToken == value) return
        sessionToken = value
        onSessionTokenChanged(value)
    }

    private companion object {
        val PING_EVERY = 10.seconds
        val SILENCE_LIMIT = 25.seconds
        val SEND_TIMEOUT = 5.seconds

        /** 1, 2, 4 and 8 seconds, then every 10 seconds. */
        fun retryDelay(failures: Int): Duration = minOf(10.seconds, 1.seconds * (1 shl minOf(failures - 1, 4)))
    }
}
