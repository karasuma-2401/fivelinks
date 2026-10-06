package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** Stands in for the server link: tests push server messages and read what was sent. */
internal class FakeConnection : ServerConnection {
    override val state = MutableStateFlow<ConnectionState>(ConnectionState.Offline)
    val incoming = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val messages: Flow<ServerMessage> = incoming
    val sent = mutableListOf<ClientMessage>()
    var started = false
        private set
    /** False makes every send fail, as when the link is down. */
    var online = true
    private var ids = 0

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
        state.value = ConnectionState.Offline
    }

    override suspend fun send(build: (messageId: String) -> ClientMessage): Boolean {
        if (!online) return false
        sent += build("t${++ids}")
        return true
    }
}

internal const val ME = "me"
internal const val YOU = "you"

/** A fresh online game between [ME] (blue) and [YOU] (red); [first] moves first. */
internal fun dealOnlineGame(first: String = ME): GameState {
    val me = Player(ME, "Me", Team.BLUE)
    val you = Player(YOU, "You", Team.RED)
    val players = if (first == ME) listOf(me, you) else listOf(you, me)
    return GameEngine.initialize(GameConfig.tactical(playerCount = 2, teams = 2, seed = 5), players)
}
