package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.PlayerView
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.toGameState
import kotlinx.coroutines.channels.Channel

/** A player as [GameServer] sees one, without a real socket; keeps every message it is sent. */
internal class FakeClient(private val server: GameServer) {
    var connection = ClientConnection(capacity = Channel.UNLIMITED)
        private set
    val inbox = mutableListOf<ServerMessage>()
    lateinit var playerId: PlayerId
    lateinit var token: String
    private var ids = 0

    fun nextId(): String = "c${++ids}"

    suspend fun send(message: ClientMessage) = server.handle(connection, message)

    /** What arrived since the last call (also kept in [inbox]). */
    fun drain(): List<ServerMessage> {
        val fresh = generateSequence { connection.outgoing.tryReceive().getOrNull() }.toList()
        inbox += fresh
        return fresh
    }

    suspend fun hello(name: String = "An"): FakeClient {
        send(ClientMessage.Hello(nextId(), playerName = name))
        val welcome = drain().only<ServerMessage.Welcome>()
        playerId = welcome.playerId
        token = welcome.sessionToken
        return this
    }

    /** The socket closes without a goodbye. */
    suspend fun drop() = server.disconnected(connection)

    /** Comes back on a new socket with the saved token. */
    suspend fun reconnect() {
        connection = ClientConnection(capacity = Channel.UNLIMITED)
        send(ClientMessage.Hello(nextId(), playerName = "ignored", sessionToken = token))
    }

    /** The latest view of the game this player received. */
    val view: PlayerView
        get() = when (val last = inbox.last { it is ServerMessage.GameSnapshot || it is ServerMessage.StateUpdate }) {
            is ServerMessage.GameSnapshot -> last.view
            is ServerMessage.StateUpdate -> last.view
            else -> error("no game yet")
        }

    val board: Board get() = inbox.lastOf<ServerMessage.GameSnapshot>().board

    /** Some legal move for this player's turn, worked out only from what they can see. */
    fun legalMove(): ClientMessage.SubmitMove {
        drain()
        val move = GameEngine.legalMoves(view.toGameState(board), playerId).first()
        return ClientMessage.SubmitMove(nextId(), move, view.turnNumber)
    }
}

internal inline fun <reified T : ServerMessage> List<ServerMessage>.only(): T = filterIsInstance<T>().single()

internal inline fun <reified T : ServerMessage> List<ServerMessage>.lastOf(): T = filterIsInstance<T>().last()
