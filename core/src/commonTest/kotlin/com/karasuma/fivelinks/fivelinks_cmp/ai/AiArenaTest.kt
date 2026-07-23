package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class AiArenaTest {

    @Test
    fun heuristicSelfPlay_hard_finishesAndCountsWins() = runTest {
        val ai = HeuristicEvaluator(random = Random(42))
        var winsRed = 0
        var winsBlue = 0
        var other = 0

        repeat(20) { gameIndex ->
            val config = GameConfig.soloVsAi(seed = 1000L + gameIndex)
            val players = listOf(
                Player(id = "p0", name = "Human", team = Team.RED, isAi = false),
                Player(id = "p1", name = "AI", team = Team.BLUE, isAi = true),
            )
            var state = GameEngine.initialize(config, players)
            var guard = 0
            while (!state.isGameOver && guard++ < 500) {
                val move = ai.chooseMove(state, state.currentPlayer.id, Difficulty.HARD)
                state = GameEngine.applyMove(state, move).getOrThrow()
            }
            when {
                !state.isGameOver -> other++
                state.winner == Team.RED -> winsRed++
                state.winner == Team.BLUE -> winsBlue++
                else -> other++
            }
        }

        println("AiArena HARD baseline: RED=$winsRed BLUE=$winsBlue other=$other")
        assertTrue(winsRed + winsBlue + other == 20)
        assertTrue(winsRed + winsBlue >= 1, "expected at least one decisive game")
    }
}
