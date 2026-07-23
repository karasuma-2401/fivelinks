package com.karasuma.fivelinks.fivelinks_cmp.route

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.model.ErrorResponse
import com.karasuma.fivelinks.fivelinks_cmp.model.NewGameRequest
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.gameRoute() {
    route("/game") {
        post("/new") {
            val request = call.receive<NewGameRequest>()
            if (request.playerCount in 2..12 && (request.teamCount == 2 || request.teamCount == 3)) {
                val config = GameConfig.forPlayer(request.playerCount, request.teamCount, request.seed)
                val players = config.teamsByPlayerIndex.mapIndexed { index, team ->
                    Player(id = "p$index", name = team.name, team = team, isAi = true)
                }
                val gameState = GameEngine.initialize(config, players)
                call.respond(HttpStatusCode.Created, gameState )
            } else {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCodes.INVALID_REQUEST, message = "Invalid Players or team count passed"))
            }
        }
    }
}