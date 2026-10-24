package com.karasuma.fivelinks.fivelinks_cmp

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.ai.AiService
import com.karasuma.fivelinks.fivelinks_cmp.online.GameServer
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineConfig
import com.karasuma.fivelinks.fivelinks_cmp.online.onlineRoute
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolVersion
import com.karasuma.fivelinks.fivelinks_cmp.route.aiRoute
import com.karasuma.fivelinks.fivelinks_cmp.route.gameRoute
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

fun main() {
    val config = OnlineConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = config.host) {
        module(config = config)
    }.start(wait = true)
}

@Serializable
data class ProtocolVersionPayload(val minor: Int, val major: Int, val name: String)

@Serializable
data class HealthCheckPayload(val status: String)

fun Application.module() {
    module(aiService = AiFacadeService(), config = OnlineConfig.fromEnvironment())
}

fun Application.module(
    aiService: AiService = AiFacadeService(),
    config: OnlineConfig = OnlineConfig.fromEnvironment(),
    gameServer: GameServer = GameServer(this, config),
) {
    install(ContentNegotiation) {
        json(ProtocolJson)
    }
    install(WebSockets) {
        // Mobile connections often die without a goodbye; pings notice within half a minute.
        pingPeriodMillis = 15_000
        timeoutMillis = 30_000
        maxFrameSize = 64 * 1024
    }

    monitor.subscribe(ApplicationStopping) {
        runBlocking {
            gameServer.shutdown()
        }
    }

    routing {
        aiRoute(aiService)
        gameRoute()
        onlineRoute(gameServer)

        get("/") {
            call.respondText(sayHello("Ktor"))
        }

        get("/health") {
            call.respondText("Health OK")
        }

        get("/health/liveness") {
            call.respond(HttpStatusCode.OK, HealthCheckPayload(status = "UP"))
        }

        get("/health/readiness") {
            val status = gameServer.status()
            val httpStatus = if (status.isShuttingDown || status.activeRooms >= status.maxRooms) {
                HttpStatusCode.ServiceUnavailable
            } else {
                HttpStatusCode.OK
            }
            call.respond(httpStatus, status)
        }

        get("/metrics") {
            call.respond(HttpStatusCode.OK, gameServer.metrics())
        }

        get("/protocol/version") {
            call.respond(
                ProtocolVersionPayload(
                    minor = ProtocolVersion.MINOR,
                    major = ProtocolVersion.MAJOR,
                    name = ProtocolVersion.STRING,
                )
            )
        }
    }
}
