package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.module
import com.karasuma.fivelinks.fivelinks_cmp.protocol.FirstPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEndReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

/** Two app sessions play through the real game server, over real sockets. */
class OnlineEndToEndTest {

    private class Phone(name: String, url: String, scope: CoroutineScope) {
        val client = OnlineClient(name, scope, url)
        val session = OnlineSession(client, scope)

        suspend fun until(what: String, predicate: (OnlineState) -> Boolean): OnlineState =
            withTimeout(10.seconds) { session.state.first(predicate) }
                .also { check(predicate(it)) { what } }

        /** Plays the first legal move, worked out from what this phone can see. */
        fun playAnyMove() {
            val game = checkNotNull(session.state.value.game)
            session.submitMove(GameEngine.legalMoves(game.state, game.view.viewerId).first())
        }
    }

    @Test
    fun twoPhones_playDropAndReconnect() = runBlocking {
        val server = embeddedServer(Netty, port = 0, host = "127.0.0.1") {
            module(AiFacadeService(neural = null))
        }.start(wait = false)
        val port = server.engine.resolvedConnectors().first().port
        val url = "ws://127.0.0.1:$port/ws"
        val host = Phone("Host", url, this)
        val guest = Phone("Guest", url, this)
        try {
            host.session.start()
            host.until("host connected") { it.connection is ConnectionState.Connected }
            host.session.createRoom(RoomSettings(firstPlayer = FirstPlayer.HOST))
            val code = assertNotNull(host.until("room opened") { it.room != null }.room).code

            guest.session.start()
            guest.until("guest connected") { it.connection is ConnectionState.Connected }
            guest.session.joinRoom(code.lowercase())
            guest.until("guest dealt in") { it.game != null }
            host.until("host dealt in") { it.game?.isMyTurn == true }

            host.playAnyMove()
            guest.until("guest sees the move") { it.game?.view?.turnNumber == 1 && it.game.isMyTurn }

            // The guest's network drops; the client reconnects and the server resumes the game.
            val guestId = guest.session.state.value.playerId
            guest.client.dropConnection()
            guest.until("guest notices the drop") { it.connection is ConnectionState.Reconnecting }
            host.until("host sees the guest away") { it.opponentAway }
            val back = guest.until("guest back in the game") {
                it.connection is ConnectionState.Connected && it.game?.isMyTurn == true
            }
            assertEquals(guestId, back.playerId)
            host.until("host sees the guest return") { !it.opponentAway }

            guest.playAnyMove()
            host.until("host sees the guest's move") { it.game?.view?.turnNumber == 2 }

            guest.session.leaveRoom()
            val end = host.until("host wins by forfeit") { it.result != null }
            assertEquals(GameEndReason.FORFEIT_LEFT, end.result?.reason)
        } finally {
            host.session.close()
            guest.session.close()
            server.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
        }
    }
}
