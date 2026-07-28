package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.ActionCodec
import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.StateEncoder
import com.karasuma.fivelinks.fivelinks_cmp.ai.export.TrainSample
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.SearchEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.toMove
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * JVM self-play exporter for Phase 6 training data.
 *
 * Disabled by default (slow). Run explicitly:
 *   ./gradlew :core:jvmTest --tests "...SelfPlayRunnerTest.generateSelfPlayData" -Dselfplay.run=true -Dselfplay.games=20
 */
class SelfPlayRunnerTest {

    private val encoder = StateEncoder()
    private val codec = ActionCodec()
    private val searchEngine = SearchEngine()
    private val heuristic = HeuristicEvaluator()
    private val json = Json { encodeDefaults = true }

    @Test
    fun generateSelfPlayData() {
        val enabled = System.getProperty("selfplay.run")?.equals("true", ignoreCase = true) == true
        if (!enabled) {
            println("Skip SelfPlayRunnerTest (pass -Dselfplay.run=true to generate data)")
            return
        }

        val totalGames = System.getProperty("selfplay.games")?.toIntOrNull() ?: 50
        val outputFile = resolveOutputFile()
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val diffConfig = DifficultyConfig.selfPlay()
        var sampleCount = 0

        repeat(totalGames) { gameIndex ->
            val config = GameConfig.soloVsAi(seed = Random.Default.nextLong() + gameIndex)
            val players = listOf(
                Player(id = "p0", name = "AI_1", team = Team.RED, isAi = true),
                Player(id = "p1", name = "AI_2", team = Team.BLUE, isAi = true),
            )
            var state = GameEngine.initialize(config, players)

            val gameHistory = mutableListOf<Pair<PlayerId, TrainSample>>()
            var turnGuard = 0

            while (!state.isGameOver && turnGuard++ < 200) {
                val currentPlayer = state.currentPlayer
                val legalMoves = GameEngine.legalMoves(state, currentPlayer.id)
                if (legalMoves.isEmpty()) break

                val visits = searchEngine.search(state, currentPlayer.id, diffConfig)
                val chosenMove = if (visits.isEmpty()) {
                    legalMoves.maxBy { heuristic.score(state, it, currentPlayer.id) }
                } else {
                    selectByVisits(visits, currentPlayer.id, diffConfig, Random.Default)
                }

                val piTensor = FloatArray(codec.maxActions)
                if (visits.isNotEmpty()) {
                    val legalMask = codec.legalMask(state, currentPlayer.id)
                    var totalVisits = 0f
                    for ((moveKey, count) in visits) {
                        val actionIdx = codec.encode(moveKey.toMove(currentPlayer.id))
                        if (!legalMask[actionIdx]) continue
                        piTensor[actionIdx] = count.toFloat()
                        totalVisits += count.toFloat()
                    }
                    if (totalVisits > 0f) {
                        for (i in piTensor.indices) {
                            if (piTensor[i] != 0f) piTensor[i] /= totalVisits
                        }
                    }
                }
                if (piTensor.all { it == 0f }) {
                    // Fallback: heuristic softmax over legal moves (keeps dataset non-empty)
                    val scores = legalMoves.map { heuristic.score(state, it, currentPlayer.id) }
                    val max = scores.max()
                    val exps = scores.map { kotlin.math.exp((it - max) / 1000.0) }
                    val sum = exps.sum().coerceAtLeast(1e-9)
                    legalMoves.forEachIndexed { i, move ->
                        piTensor[codec.encode(move)] = (exps[i] / sum).toFloat()
                    }
                }

                gameHistory.add(
                    currentPlayer.id to TrainSample(
                        encoderVersion = encoder.version,
                        state = encoder.encode(state, currentPlayer.team),
                        pi = piTensor,
                        z = 0f,
                    ),
                )

                state = GameEngine.applyMove(state, chosenMove).getOrThrow()
            }

            val winnerTeam = state.winner
            for ((playerId, sample) in gameHistory) {
                val playerTeam = state.players.first { it.id == playerId }.team
                sample.z = when {
                    winnerTeam == null -> 0f
                    winnerTeam == playerTeam -> 1f
                    else -> -1f
                }
                outputFile.appendText(json.encodeToString(sample) + "\n")
                sampleCount++
            }
            println("Done game ${gameIndex + 1} / $totalGames (samples=$sampleCount)")
        }

        println("Self-play finished: $sampleCount samples → ${outputFile.absolutePath}")
        assertTrue(sampleCount > 0, "Expected at least one training sample")
    }

    private fun resolveOutputFile(): File {
        System.getProperty("selfplay.out")?.let { return File(it) }
        val root = findRepoRoot()
        return File(root, "ml/data/selfplay_dataset.jsonl")
    }

    private fun findRepoRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            val marker = File(dir, "ml/train")
            val settings = File(dir, "settings.gradle.kts")
            if (marker.isDirectory || settings.exists()) return dir
            dir = dir.parentFile ?: return File(System.getProperty("user.dir"))
        }
        return File(System.getProperty("user.dir"))
    }
}
