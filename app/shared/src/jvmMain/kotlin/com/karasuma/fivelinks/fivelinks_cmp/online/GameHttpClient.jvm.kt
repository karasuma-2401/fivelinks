package com.karasuma.fivelinks.fivelinks_cmp.online

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import java.util.concurrent.TimeUnit

internal actual fun createGameHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
    install(HttpTimeout) {
        connectTimeoutMillis = 60_000
        requestTimeoutMillis = 65_000
        socketTimeoutMillis = 65_000
    }
    engine {
        config {
            connectTimeout(60, TimeUnit.SECONDS)
            readTimeout(65, TimeUnit.SECONDS)
            writeTimeout(65, TimeUnit.SECONDS)
        }
    }
}
