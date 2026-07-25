package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.ai.DifficultyConfig
import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.TacticalEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.math.tanh
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class SearchEngine(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
    private val tactical: TacticalEngine = TacticalEngine(heuristic),
    private val determinizer: Determinizer = Determinizer(),
) {
    fun search(
        realState: GameState,
        playerId: PlayerId,
        config: DifficultyConfig,
    ): Map<MoveKey, Int> {
        val aggregate = mutableMapOf<MoveKey, Int>()
        val simsPerWorld = (config.maxSimulations / config.determinizations.coerceAtLeast(1))
            .coerceAtLeast(1)
        val deadline = TimeSource.Monotonic.markNow() + config.timeBudgetMs.milliseconds

        repeat(config.determinizations) {
            if (deadline.hasPassedNow()) return@repeat
            val world = determinizer.sample(realState, playerId)
            val rootVisits = runMcts(world, playerId, simsPerWorld, config)
            for ((key, n) in rootVisits) {
                aggregate[key] = (aggregate[key] ?: 0) + n
            }
        }
        return aggregate
    }

    private fun runMcts(
        rootState: GameState,
        rootPlayerId: PlayerId,
        simulations: Int,
        config: DifficultyConfig,
    ): Map<MoveKey, Int> {
        val rootTeam = rootState.players.first { it.id == rootPlayerId }.team
        val root = MctsNode()
        root.untried = GameEngine.legalMoves(rootState, rootState.currentPlayer.id).toMutableList()
        root.expanded = true

        repeat(simulations) {
            var state = rootState
            var node = root
            val path = mutableListOf(node)

            // SELECT
            while (node.children.isNotEmpty() && node.untried.isEmpty() && !state.isGameOver) {
                node = selectChild(node, config.cPuct)
                val move = node.moveFromParent!!
                state = GameEngine.applyMove(state, move).getOrThrow()
                path += node
            }

            // EXPAND
            if (!state.isGameOver) {
                val toPlay = state.currentPlayer.id
                if (!node.expanded) {
                    node.untried = GameEngine.legalMoves(state, toPlay).toMutableList()
                    node.expanded = true
                }
                if (node.untried.isNotEmpty()) {
                    node.untried = tactical.orderMoves(state, toPlay, node.untried).toMutableList()
                    val move = node.untried.removeAt(0)
                    val key = MoveKey.from(move)
                    val child = MctsNode(parent = node, moveFromParent = move)
                    val score = heuristic.score(state, move, toPlay)
                    child.prior = softPrior(score)
                    node.children[key] = child
                    state = GameEngine.applyMove(state, move).getOrThrow()
                    node = child
                    path += node
                }
            }

            // EVALUATE
            val value = evaluateLeaf(state, rootTeam)

            // BACKUP
            for (n in path) {
                n.visitCount += 1
                n.totalValue += value
            }
        }

        return root.children.mapValues { it.value.visitCount }
    }

    private fun evaluateLeaf(state: GameState, rootTeam: Team): Double {
        val winner = state.winner
        if (winner != null) {
            return when (winner) {
                rootTeam -> 1.0
                else -> -1.0
            }
        }
        val mySeq = state.sequencesOf(rootTeam)
        val oppSeq = state.config.teams.filter { it != rootTeam }.sumOf { state.sequencesOf(it) }
        val myOpen = com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.ThreatDetector.countOpenFours(state, rootTeam)
        val oppOpen = state.config.teams.filter { it != rootTeam }
            .sumOf { com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.ThreatDetector.countOpenFours(state, it) }
        val raw = (mySeq - oppSeq) * 2.0 + (myOpen - oppOpen) * 0.5
        return tanh(raw)
    }

    private fun softPrior(score: Double): Double {
        return 1.0 / (1.0 + kotlin.math.exp(-score / 1000.0))
    }
}
