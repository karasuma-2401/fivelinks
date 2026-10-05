package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * One open socket. [GameServer] queues messages here without waiting on the network;
 * the transport drains [outgoing] and closes the socket once the queue is closed.
 */
class ClientConnection(capacity: Int = 64) {
    private val queue = Channel<ServerMessage>(capacity)
    val outgoing: ReceiveChannel<ServerMessage> get() = queue

    internal var session: PlayerSession? = null

    /** Queues [message]; a client too slow to keep up is dropped. */
    fun send(message: ServerMessage) {
        if (queue.trySend(message).isFailure) close()
    }

    /** Delivers what is already queued, then closes the socket. */
    fun close() {
        queue.close()
    }
}
