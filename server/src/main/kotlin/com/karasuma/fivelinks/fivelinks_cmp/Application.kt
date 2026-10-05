package com.karasuma.fivelinks.fivelinks_cmp

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.ai.AiService
import com.karasuma.fivelinks.fivelinks_cmp.online.GameServer
import com.karasuma.fivelinks.fivelinks_cmp.online.onlineRoute
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolVersion
import com.karasuma.fivelinks.fivelinks_cmp.route.aiRoute
import com.karasuma.fivelinks.fivelinks_cmp.route.gameRoute
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.Serializable

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Serializable
data class ProtocolVersionPayload(val minor: Int, val major: Int, val name: String)

fun Application.module() {
    module(AiFacadeService())
}

fun Application.module(aiService: AiService) {
    install(ContentNegotiation) {
        json(ProtocolJson)
    }
    install(WebSockets) {
        // Mobile connections often die without a goodbye; pings notice within half a minute.
        pingPeriodMillis = 15_000
        timeoutMillis = 30_000
        maxFrameSize = 64 * 1024
    }
    val gameServer = GameServer(this)
    routing {
        aiRoute(aiService)
        gameRoute()
        onlineRoute(gameServer)

        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        get("/health") {
            call.respondText("Heath OK")
        }
        get("/protocol/version") {
            call.respond(
                ProtocolVersionPayload(
                    minor = ProtocolVersion.MINOR,
                    major = ProtocolVersion.MAJOR,
                    name = ProtocolVersion.STRING
                )
            )
        }
    }
}
