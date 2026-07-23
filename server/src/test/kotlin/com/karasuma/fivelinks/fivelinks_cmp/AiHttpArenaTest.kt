package com.karasuma.fivelinks.fivelinks_cmp

import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.model.AiMoveRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.NewGameRequest
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class AiHttpArenaTest {

    private fun playersMatchingGameRoute(config: GameConfig): List<Player> =
        config.teamsByPlayerIndex.mapIndexed { index, team ->
            Player(id = "p$index", name = team.name, team = team, isAi = true)
        }

    private suspend fun playInProcess(seed: Long): Team? {
        val config = GameConfig.forPlayer(2, 2, seed)
        val ai = HeuristicEvaluator(random = Random(0))
        var state = GameEngine.initialize(config, playersMatchingGameRoute(config))
        var guard = 0
        while (!state.isGameOver && guard++ < 500) {
            val move = ai.chooseMove(state, state.currentPlayer.id, Difficulty.HARD)
            state = GameEngine.applyMove(state, move).getOrThrow()
        }
        return state.winner
    }

    @Test
    fun httpArena_hard_matchesInProcessForSameSeeds() = testApplication {
        application { module() }

        suspend fun playViaHttp(seed: Long): Team? {
            val created = client.post("/game/new") {
                contentType(ContentType.Application.Json)
                setBody(
                    ProtocolJson.encodeToString(
                        NewGameRequest.serializer(),
                        NewGameRequest(playerCount = 2, teamCount = 2, seed = seed),
                    ),
                )
            }
            assertEquals(HttpStatusCode.Created, created.status)
            var state = ProtocolJson.decodeFromString(GameState.serializer(), created.bodyAsText())
            var guard = 0
            while (!state.isGameOver && guard++ < 500) {
                val res = client.post("/ai/move") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ProtocolJson.encodeToString(
                            AiMoveRequest.serializer(),
                            AiMoveRequest(state, state.currentPlayer.id, Difficulty.HARD),
                        ),
                    )
                }
                assertEquals(HttpStatusCode.OK, res.status, res.bodyAsText())
                val move = ProtocolJson.decodeFromString(Move.serializer(), res.bodyAsText())
                state = GameEngine.applyMove(state, move).getOrThrow()
            }
            return state.winner
        }

        for (seed in 1000L..1004L) {
            assertEquals(
                playInProcess(seed),
                playViaHttp(seed),
                "winner mismatch for seed=$seed",
            )
        }
    }
}
