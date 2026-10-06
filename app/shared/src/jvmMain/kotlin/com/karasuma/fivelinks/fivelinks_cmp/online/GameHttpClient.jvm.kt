package com.karasuma.fivelinks.fivelinks_cmp.online

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

internal actual fun createGameHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
}
