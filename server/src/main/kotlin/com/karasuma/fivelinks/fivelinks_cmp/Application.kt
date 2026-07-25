package com.karasuma.fivelinks.fivelinks_cmp

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.ai.AiService
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
import kotlinx.serialization.Serializable

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Serializable
data class ProtocolVersionPayload(val minor: Int, val major: Int, val name: String)
fun Application.module() {
    install(ContentNegotiation) {
        json(ProtocolJson)
    }
    val aiService: AiService = AiFacadeService()
    routing {
        aiRoute(aiService)
        gameRoute()

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

