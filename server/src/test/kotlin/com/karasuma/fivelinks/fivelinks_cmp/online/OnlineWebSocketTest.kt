package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.module
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.FirstPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.PlayerView
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class OnlineWebSocketTest {

    @Test
    fun twoPlayers_finishAWholeGameOverWebSockets() = testApplication {
        application { module(AiFacadeService(neural = null)) }
        val client = createClient { install(WebSockets) }
        val host = Bot("Host", seed = 1)
        val guest = Bot("Guest", seed = 2)
        val roomCode = CompletableDeferred<String>()

        withTimeout(60.seconds) {
            coroutineScope {
                launch {
                    client.webSocket("/ws") {
                        host.connect(this)
                        host.send(ClientMessage.CreateRoom(host.nextId(), RoomSettings(firstPlayer = FirstPlayer.HOST)))
                        roomCode.complete(host.receive<ServerMessage.RoomState>().roomCode)
                        host.playToTheEnd()
                    }
                }
                launch {
                    client.webSocket("/ws") {
                        guest.connect(this)
                        guest.send(ClientMessage.JoinRoom(guest.nextId(), roomCode.await()))
                        guest.playToTheEnd()
                    }
                }
            }
        }

        val result = assertNotNull(host.result)
        assertEquals(result, guest.result)
        assertTrue(host.movesPlayed > 0 && guest.movesPlayed > 0)
        assertEquals(host.lastView?.chips, guest.lastView?.chips)
        assertTrue(host.lastView?.isGameOver == true)
        // Nobody was ever sent the deck or another player's hand.
        for (frame in host.frames + guest.frames) {
            assertFalse("\"deck\"" in frame, frame)
            assertFalse("\"hands\"" in frame, frame)
        }
    }

    /** Plays random legal moves, chosen only from what the server lets it see. */
    private class Bot(val name: String, seed: Long) {
        private val random = Random(seed)
        private lateinit var socket: DefaultClientWebSocketSession
        private var ids = 0
        private var board: Board? = null
        lateinit var playerId: PlayerId
        var lastView: PlayerView? = null
        var result: GameResult? = null
        var movesPlayed = 0
        val frames = mutableListOf<String>()

        fun nextId(): String = "$name-${++ids}"

        suspend fun connect(session: DefaultClientWebSocketSession) {
            socket = session
            send(ClientMessage.Hello(nextId(), playerName = name))
            playerId = receive<ServerMessage.Welcome>().playerId
        }

        suspend fun send(message: ClientMessage) {
            socket.send(Frame.Text(ProtocolJson.encodeToString(ClientMessage.serializer(), message)))
        }

        suspend fun next(): ServerMessage {
            val text = (socket.incoming.receive() as Frame.Text).readText()
            frames += text
            return ProtocolJson.decodeFromString(ServerMessage.serializer(), text)
        }

        /** Skips ahead to the next message of type [T]. */
        suspend inline fun <reified T : ServerMessage> receive(): T {
            while (true) {
                val message = next()
                if (message is T) return message
            }
        }

        suspend fun playToTheEnd() {
            while (result == null) {
                when (val message = next()) {
                    is ServerMessage.GameSnapshot -> {
                        board = message.board
                        lastView = message.view
                    }
                    is ServerMessage.StateUpdate -> lastView = message.view
                    is ServerMessage.GameOver -> result = message.result
                    is ServerMessage.Rejected -> error("$name was refused: ${message.reason} ${message.message}")
                    else -> continue
                }
                val view = lastView ?: continue
                if (result == null && !view.isGameOver && view.currentPlayer.id == playerId) {
                    val moves = GameEngine.legalMoves(view.asGameState(checkNotNull(board)), playerId)
                    send(ClientMessage.SubmitMove(nextId(), moves.random(random), view.turnNumber))
                    movesPlayed++
                }
            }
        }
    }
}
