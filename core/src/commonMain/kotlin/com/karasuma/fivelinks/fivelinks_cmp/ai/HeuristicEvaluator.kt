package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.ThreatDetector
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardLines
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.isTwoEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.domain.lineStatus
import kotlin.math.abs
import kotlin.random.Random

class HeuristicEvaluator(
    private val weights: HeuristicWeights = HeuristicWeights.default,
    private val random: Random = Random.Default
) : AiService {
    override suspend fun chooseMove(
        state: GameState,
        playerId: PlayerId,
        difficulty: Difficulty
    ): Move {
        val moves = GameEngine.legalMoves(state, playerId)
        require(moves.isNotEmpty()) { "No legal moves for $playerId" }
        val scored = moves.map { it to score(state, it, playerId) }
        val sorted = scored.sortedByDescending { it.second }

        return when (difficulty) {
            Difficulty.HARD -> sorted.first().first
            Difficulty.MEDIUM, Difficulty.EASY -> SoftmaxPicker.pick(
                sorted.take(difficulty.topK),
                difficulty.temperature
            )
        }
    }

    fun score(state: GameState, move: Move, playerId: PlayerId): Double {
        val player = state.players.first { it.id == playerId }
        val myTeam = player.team
        val oppTeams = (state.config.teams - myTeam).toList()

        val result = GameEngine.applyMove(state, move)
        if (result.isFailure) return Double.NEGATIVE_INFINITY
        val after = result.getOrThrow()

        // --- Critical Priorities ---
        if (after.winner == myTeam) {
            return 1_000_000.0
        }

        val beforeMyOpen4 = ThreatDetector.countOpenFours(state, myTeam)
        val beforeOppOpen4 = oppTeams.sumOf { ThreatDetector.countOpenFours(state, it) }
        val afterMyOpen4 = ThreatDetector.countOpenFours(after, myTeam)
        val afterOppOpen4 = oppTeams.sumOf { ThreatDetector.countOpenFours(after, it) }

        var score = 0.0

        // Blocked a threat - this is a very high priority
        if (beforeOppOpen4 > afterOppOpen4) {
            score += weights.blockOpponentOpenFour * (beforeOppOpen4 - afterOppOpen4)
        }

        // --- Major Priorities ---
        val newSeqs = after.completedSequence.size - state.completedSequence.size
        if (newSeqs > 0) {
            score += newSeqs * weights.completeSequence
        }

        // Created a new open-four
        if (afterMyOpen4 > beforeMyOpen4) {
            score += (afterMyOpen4 - beforeMyOpen4) * weights.selfOpenFour
        }

        if (afterMyOpen4 - beforeMyOpen4 >= 2) {
            score += weights.doubleThreat
        }

        // --- Minor Priorities & Penalties ---
        when (move) {
            is Move.Place -> {
                score += extensionAround(after, move.position, myTeam) * weights.extendSelfPerChip
                score += blockingAround(state, move.position, oppTeams) *
                        weights.blockOpponentPerChip * weights.defensiveMultiplier
                score += centerBias(move.position) * weights.centerControl
                score += adjacentCornerCount(move.position) * weights.cornerAdjacency
                if (move.card.isTwoEyedJack() && newSeqs == 0 && afterMyOpen4 == beforeMyOpen4) {
                    score += weights.wasteWildJack
                }
            }
            is Move.Remove -> {
                // Add center bias as a tie-breaker for remove moves
                score += centerBias(move.position) * weights.centerControl
                val wasThreat = (beforeOppOpen4 - afterOppOpen4) > 0
                if (!wasThreat) score += weights.wasteRemoveJack
            }
            is Move.SwapDeadCard -> {
                score += weights.swapDeadCard
            }
        }
        return score
    }

    private fun extensionAround(state: GameState, pos: BoardPosition, team: Team): Int {
        val lines = BoardLines.throughPosition[pos] ?: return 0
        var chips = 0
        for (line in lines) {
            val s = lineStatus(state, line, team)
            if (s.opponent > 0) continue
            chips += (s.own - 1).coerceAtLeast(0)
        }
        return chips
    }

    private fun blockingAround(state: GameState, pos: BoardPosition, oppTeams: List<Team>): Int {
        val lines = BoardLines.throughPosition[pos] ?: return 0
        var total = 0
        for (line in lines) {
            for (t in oppTeams) {
                val s = lineStatus(state, line, t)
                if (s.own >= 2 && s.opponent == 0) total += s.own
            }
        }
        return total
    }

    private fun centerBias(p: BoardPosition): Int {
        val dr = abs(p.row - 4.5)
        val dc = abs(p.column - 4.5)
        val dist = (dr + dc).toInt()
        return (9 - dist).coerceAtLeast(0)
    }

    private fun adjacentCornerCount(p: BoardPosition): Int =
        BoardPosition.corners.count { c -> abs(c.row - p.row) <= 1 && abs(c.column - p.column) <= 1 }
}