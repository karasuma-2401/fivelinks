package com.karasuma.fivelinks.fivelinks_cmp.route

import com.karasuma.fivelinks.fivelinks_cmp.database.LeaderboardDatabase
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.leaderboardRoute(db: LeaderboardDatabase) {
    route("/api") {
        get("/leaderboard") {
            val playerId = call.request.queryParameters["playerId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val response = db.getTopPlayers(limit = limit, currentPlayerId = playerId)
            call.respond(HttpStatusCode.OK, response)
        }

        post("/player/sync") {
            try {
                val req = call.receive<PlayerSyncRequest>()
                val updated = db.syncPlayer(req)
                call.respond(HttpStatusCode.OK, PlayerSyncResponse(success = true, updatedEntry = updated))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }
    }
}

