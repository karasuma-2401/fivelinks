package com.karasuma.fivelinks.fivelinks_cmp.route

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.ai.AiService
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.model.AiMoveRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.AiMoveResponse
import com.karasuma.fivelinks.fivelinks_cmp.model.ErrorResponse
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.aiRoute(ai: AiService = AiFacadeService()) {
    route("/ai") {
        post("/move") {
            val request = call.receive<AiMoveRequest>()
            val move = runCatching {
                ai.chooseMove(request.gameState, request.playerId, request.difficulty)
            }.getOrElse {
                val errorResponse = ErrorResponse(ErrorCodes.INVALID_REQUEST, "Invalid request: ${it.message}")
                call.respond(HttpStatusCode.BadRequest, errorResponse)
                return@post
            }
            call.respond(HttpStatusCode.OK, AiMoveResponse(move))
        }
    }

}