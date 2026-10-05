package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Online play: one WebSocket per player, one JSON message per text frame. */
fun Route.onlineRoute(server: GameServer) {
    webSocket("/ws") {
        val connection = ClientConnection()
        val writer = launch {
            try {
                for (message in connection.outgoing) {
                    outgoing.send(Frame.Text(ProtocolJson.encodeToString(ServerMessage.serializer(), message)))
                }
                // The server let go of this socket: replaced by a newer one, or an incompatible client.
                close(CloseReason(CloseReason.Codes.NORMAL, "Closed by server"))
            } catch (e: ClosedSendChannelException) {
                // The client went away first; nothing left to deliver.
            }
        }
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val message = try {
                    ProtocolJson.decodeFromString(ClientMessage.serializer(), frame.readText())
                } catch (e: IllegalArgumentException) {
                    // Malformed JSON or an unknown message type (SerializationException is one of these).
                    null
                }
                if (message == null) server.unreadable(connection) else server.handle(connection, message)
            }
        } finally {
            withContext(NonCancellable) { server.disconnected(connection) }
            connection.close()
            writer.cancel()
        }
    }
}
