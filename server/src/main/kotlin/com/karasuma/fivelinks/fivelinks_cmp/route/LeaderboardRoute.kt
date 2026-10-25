package com.karasuma.fivelinks.fivelinks_cmp.route

import com.karasuma.fivelinks.fivelinks_cmp.database.LeaderboardDatabase
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncResponse
import com.karasuma.fivelinks.fivelinks_cmp.online.RateLimiter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.leaderboardRoute(db: LeaderboardDatabase) {
    val readRateLimiter = RateLimiter(limit = 30, windowMillis = 60_000L)
    val syncRateLimiter = RateLimiter(limit = 10, windowMillis = 60_000L)

    route("/api") {
        get("/leaderboard") {
            val clientIp = call.request.origin.remoteHost
            if (!readRateLimiter.tryAcquire("read:$clientIp")) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limit exceeded"))
                return@get
            }
            val playerId = call.request.queryParameters["playerId"]
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 100)
            val response = db.getTopPlayers(limit = limit, currentPlayerId = playerId)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/player/sync") {
            val clientIp = call.request.origin.remoteHost
            if (!syncRateLimiter.tryAcquire("sync:$clientIp")) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limit exceeded"))
                return@post
            }
            try {
                val req = call.receive<PlayerSyncRequest>()
                if (!syncRateLimiter.tryAcquire("sync_player:${req.playerId.take(64)}")) {
                    call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Player sync rate limit exceeded"))
                    return@post
                }
                val updated = db.syncPlayer(req, isServerAuthoritative = false)
                call.respond(HttpStatusCode.OK, PlayerSyncResponse(success = true, updatedEntry = updated))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }
    }
}

