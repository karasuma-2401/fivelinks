package com.karasuma.fivelinks.fivelinks_cmp.online

import io.ktor.client.HttpClient

/** An HTTP client with WebSockets, on this platform's engine (OkHttp, Js or Darwin). */
internal expect fun createGameHttpClient(): HttpClient
