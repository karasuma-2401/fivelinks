package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.ai.DifficultyConfig
import com.karasuma.fivelinks.fivelinks_cmp.ai.eval.IEvaluationEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.TacticalEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class SearchEngine(
    private val tactical: TacticalEngine = TacticalEngine(),
    private val determinizer: Determinizer = Determinizer(),
) {
    suspend fun search(
        realState: GameState,
        playerId: PlayerId,
        config: DifficultyConfig,
        evaluationEngine: IEvaluationEngine,
    ): Map<MoveKey, Int> {
        val aggregate = mutableMapOf<MoveKey, Int>()
        val simsPerWorld = (config.maxSimulations / config.determinizations.coerceAtLeast(1))
            .coerceAtLeast(1)
        val deadline = TimeSource.Monotonic.markNow() + config.timeBudgetMs.milliseconds

        repeat(config.determinizations) {
            if (deadline.hasPassedNow()) return@repeat
            val world = determinizer.sample(realState, playerId)
            val rootVisits = runMcts(world, playerId, simsPerWorld, config, deadline, evaluationEngine)
            for ((key, n) in rootVisits) {
                aggregate[key] = (aggregate[key] ?: 0) + n
            }
        }
        return aggregate
    }

    private suspend fun runMcts(
        rootState: GameState,
        rootPlayerId: PlayerId,
        simulations: Int,
        config: DifficultyConfig,
        deadline: TimeMark,
        evaluationEngine: IEvaluationEngine,
    ): Map<MoveKey, Int> {
        val rootTeam = rootState.players.first { it.id == rootPlayerId }.team
        val root = MctsNode()

        for (i in 0 until simulations) {
            // Check for cancellation and time budget.
            if (i % 16 == 0) {
                yield()
                if (deadline.hasPassedNow()) break
            }

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
                    val legalMoves = GameEngine.legalMoves(state, toPlay)
                    val ordered = tactical.orderMoves(state, toPlay, legalMoves)
                    val eval = evaluationEngine.evaluate(state, rootTeam, toPlay, ordered, wantPriors = true)
                    ordered.forEachIndexed { index, move ->
                        val key = MoveKey.from(move)
                        val child = MctsNode(parent = node, moveFromParent = move)
                        child.prior = eval.priors?.getOrNull(index)?.toDouble()
                            ?: (1.0 / ordered.size.coerceAtLeast(1))
                        node.children[key] = child
                    }
                    node.untried = ordered.toMutableList()
                    node.expanded = true
                }
                if (node.untried.isNotEmpty()) {
                    val move = node.untried.removeAt(0)
                    val key = MoveKey.from(move)
                    val child = node.children[key] ?: MctsNode(parent = node, moveFromParent = move).also {
                        node.children[key] = it
                    }
                    state = GameEngine.applyMove(state, move).getOrThrow()
                    node = child
                    path += node
                }
            }

            // EVALUATE
            val value = evaluationEngine.evaluate(
                state,
                rootTeam,
                state.currentPlayer.id,
                emptyList(),
                wantPriors = false,
            ).value

            // BACKUP
            for (n in path) {
                n.visitCount += 1
                val teamOfNodePlayer = state.players.find { it.id == n.moveFromParent?.playerId }?.team
                if (teamOfNodePlayer != null && teamOfNodePlayer != rootTeam) {
                    n.totalValue -= value
                } else {
                    n.totalValue += value
                }
            }
        }

        return root.children
            .mapValues { it.value.visitCount }
            .filterValues { it > 0 }
    }
}