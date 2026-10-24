package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.TimeSource

class AiBenchmarkArenaTest {

    @Test
    fun `hybridHard_vs_heuristicHard`() = runTest(timeout = Duration.INFINITE) {
        // Hybrid (Neural + Heuristic)
        val hybridAi = AiFacadeService() // Default constructor uses neural if available

        // Heuristic-only (by passing a null neural engine)
        val heuristicAi = AiFacadeService(neural = null)

        var hybridWins = 0
        var heuristicWins = 0
        var draws = 0
        val latencies = mutableListOf<Long>()
        // Keep default smoke count small so `:core:jvmTest` completes in seconds;
        // raise to 100 when running a full model promotion benchmark.
        val totalGames = 2

        println("Starting Arena: Hybrid HARD vs Heuristic HARD ($totalGames games, Classic + Tactical)...")

        repeat(totalGames) { gameIndex ->
            val seed = 1000L + gameIndex
            val players = listOf(
                Player("p0", "Hybrid", Team.RED, true),
                Player("p1", "Heuristic", Team.BLUE, true)
            )
            val config = if (gameIndex % 2 == 0) {
                GameConfig.forPlayer(2, 2, seed = seed)
            } else {
                GameConfig.tactical(playerCount = 2, teams = 2, seed = seed).copy(sequenceToWin = 1)
            }
            var state = GameEngine.initialize(config, players)
            var guard = 0

            while (!state.isGameOver && guard++ < 500) {
                state = GameEngine.skipStuckTurns(state).state
                if (state.isGameOver) break

                val ai = if (state.currentPlayer.id == "p0") hybridAi else heuristicAi
                val difficulty = Difficulty.HARD

                val mark = TimeSource.Monotonic.markNow()
                val chosenMove = ai.chooseMove(state, state.currentPlayer.id, difficulty)
                val elapsed = mark.elapsedNow().inWholeMilliseconds

                if (state.currentPlayer.id == "p0") {
                    latencies.add(elapsed)
                }
                state = GameEngine.applyMove(state, chosenMove).getOrThrow()
            }

            when (state.winner) {
                Team.RED -> hybridWins++
                Team.BLUE -> heuristicWins++
                else -> draws++
            }
            println("... Game ${gameIndex + 1} finished (winner=${state.winner}, turns=${state.turnNumber}).")
        }

        println("\n--- Arena Results ---")
        println("Hybrid Wins: $hybridWins (${formatPercent(hybridWins, totalGames)}%)")
        println("Heuristic Wins: $heuristicWins (${formatPercent(heuristicWins, totalGames)}%)")
        println("Draws: $draws")
        println("\n--- Hybrid AI Latency (ms) ---")
        if (latencies.isNotEmpty()) {
            println("Average: ${latencies.average().toLong()}")
            println("P95: ${latencies.sorted().getOrNull((latencies.size * 0.95).toInt())}")
            println("Max: ${latencies.maxOrNull()}")
        } else {
            println("No latency data collected.")
        }
        assertTrue(latencies.isNotEmpty(), "Expected Hybrid AI to record move latencies")
    }

    private fun formatPercent(value: Int, total: Int): String {
        if (total == 0) return "0.0"
        val percent = 100.0 * value / total
        // Simple rounding to one decimal place, platform-agnostic
        return ((percent * 10).toInt() / 10.0).toString()
    }
}
