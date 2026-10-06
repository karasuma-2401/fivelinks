package com.karasuma.fivelinks.fivelinks_cmp.online

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets

internal actual fun createGameHttpClient(): HttpClient = HttpClient(Darwin) {
    install(WebSockets)
}
