package com.karasuma.fivelinks.fivelinks_cmp.ai.tactical

import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

class TacticalEngine(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator()
) {
    fun findForceMove(state: GameState, playerId: PlayerId): Move? {
        val player = state.players.first { it.id == playerId }
        val myTeam = player.team
        val moves = GameEngine.legalMoves(state, playerId)
        if (moves.isEmpty()) return null

        // 1. Winning now
        val winningMoves = moves.filter { move ->
            val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
            after.winner == myTeam || (after.sequencesOf(myTeam) > state.sequencesOf(myTeam) && after.sequencesOf(myTeam) >= state.config.sequenceToWin)
        }
        if (winningMoves.isNotEmpty()) {
            return winningMoves.maxBy { heuristic.score(state, it, playerId) }
        }

        // 2. Block opponent's open-four
        val oppTeams = state.config.teams - myTeam
        val dangerCells = oppTeams.flatMap { ThreatDetector.openFoursEmptyCells(state, it) }.toSet()
        val beforeThreat = oppTeams.sumOf { ThreatDetector.countOpenFours(state, it) }
        if (dangerCells.isNotEmpty() || beforeThreat > 0) {
            val blockPlaces = moves.filterIsInstance<Move.Place>().filter { it.position in dangerCells }
            if (blockPlaces.isNotEmpty()) {
                return blockPlaces.maxBy { heuristic.score(state, it, playerId) }
            }

            val removes = moves.filterIsInstance<Move.Remove>()
            if (removes.isNotEmpty()) {
                val usefulRemoves = removes.filter { move ->
                    val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
                    val afterThreat = oppTeams.sumOf { ThreatDetector.countOpenFours(after, it) }
                    afterThreat < beforeThreat
                }
                if (usefulRemoves.isNotEmpty()) {
                    return findBestRemoveTarget(usefulRemoves) { it.position }
                }
            }

            val craftBlockPlaces = moves.filterIsInstance<Move.CraftPlace>().filter { it.position in dangerCells }
            if (craftBlockPlaces.isNotEmpty()) {
                return craftBlockPlaces.maxBy { heuristic.score(state, it, playerId) }
            }

            val craftRemoves = moves.filterIsInstance<Move.CraftRemove>()
            if (craftRemoves.isNotEmpty()) {
                val usefulCraftRemoves = craftRemoves.filter { move ->
                    val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
                    val afterThreat = oppTeams.sumOf { ThreatDetector.countOpenFours(after, it) }
                    afterThreat < beforeThreat
                }
                if (usefulCraftRemoves.isNotEmpty()) {
                    return findBestRemoveTarget(usefulCraftRemoves) { it.position }
                }
            }

            val wipes = moves.filterIsInstance<Move.DivineWipe>().filter { move ->
                val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
                val afterThreat = oppTeams.sumOf { ThreatDetector.countOpenFours(after, it) }
                afterThreat < beforeThreat
            }
            if (wipes.isNotEmpty()) {
                return wipes.first()
            }
        }

        // 3. Create an open-four
        val creatingMoves = moves.filter { move ->
            val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
            ThreatDetector.countOpenFours(after, myTeam) > ThreatDetector.countOpenFours(state, myTeam)
        }
        if (creatingMoves.isNotEmpty()) {
            return creatingMoves.maxBy { heuristic.score(state, it, playerId) }
        }

        return null
    }

    private fun <T : Move> findBestRemoveTarget(moves: List<T>, positionOf: (T) -> BoardPosition): T {
        if (moves.size == 1) return moves.first()

        val positions = moves.map(positionOf)
        val avgRow = positions.map { it.row }.average()
        val avgCol = positions.map { it.column }.average()

        return moves.minBy {
            val pos = positionOf(it)
            val dr = pos.row - avgRow
            val dc = pos.column - avgCol
            dr * dr + dc * dc // Squared Euclidean distance to the center
        }
    }

    fun orderMoves(state: GameState, playerId: PlayerId, moves: List<Move>): List<Move> =
        moves.sortedByDescending { heuristic.score(state, it, playerId) }
}
