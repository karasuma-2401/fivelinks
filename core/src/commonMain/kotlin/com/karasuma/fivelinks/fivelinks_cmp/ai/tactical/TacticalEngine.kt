package com.karasuma.fivelinks.fivelinks_cmp.ai.tactical

import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

class TacticalEngine (
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator()
) {
    /** return ve nuoc bat buoc phai di theo thu tu
     * 1. Win ngay lap tuc -> completedSequence++ | winner = team
     * 2. Can doi thu khi co open4 (thoa man khi co place/remove)
     * 3. Tao open4 neu khong co kha nang thua ngay lap tuc*/
    fun findForceMove(state: GameState, playerId: PlayerId): Move? {
        val player = state.players.first { it.id == playerId }
        val myTeam = player.team
        val moves = GameEngine.legalMoves(state, playerId)
        if (moves.isEmpty()) return null

        // 1. Wining now
        for (move in moves) {
            val after = GameEngine.applyMove(state, move).getOrNull() ?: continue
            if (after.winner == myTeam) return move
            if (after.sequencesOf(myTeam) > state.sequencesOf(myTeam) &&
                after.sequencesOf(myTeam) >= state.config.sequenceToWin
            ) return move
        }

        // 2. Block opp have open4
        val oppTeams = state.config.teams - myTeam
        val dangerCells = oppTeams.flatMap { ThreatDetector.openFoursEmptyCells(state, it) }.toSet()
        if (dangerCells.isNotEmpty()) {
            val blockPlace = moves.filterIsInstance<Move.Place>()
                .filter { it.position in dangerCells }
            if (blockPlace.isNotEmpty()) {
                return blockPlace.maxBy { heuristic.score(state, it, playerId) }
            }
            // case One-eyed jack -> remove chip that creates the threat (heuristic score)
            val removes = moves.filterIsInstance<Move.Remove>()
            if (removes.isNotEmpty()) {
                val beforeThreat = oppTeams.sumOf { ThreatDetector.countOpenFours(state, it) }
                val useful = removes.filter { move ->
                    val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
                    val afterThreat = oppTeams.sumOf { ThreatDetector.countOpenFours(after, it) }
                    afterThreat < beforeThreat
                }
                if (useful.isNotEmpty()) {
                    return useful.maxBy { heuristic.score(state, it, playerId) }
                }
            }
        }
        // 3. Create an open4
        val creating = moves.filter { move ->
            val after = GameEngine.applyMove(state, move).getOrNull() ?: return@filter false
            ThreatDetector.countOpenFours(after, myTeam) > ThreatDetector.countOpenFours(state, myTeam)
        }
        if (creating.isNotEmpty()) {
            return  creating.maxBy { heuristic.score(state, it, playerId) }
        }
        return null
    }
    // move order for MCTS
    fun orderMoves(state: GameState, playerId: PlayerId, moves: List<Move>): List<Move> =
        moves.sortedByDescending { heuristic.score(state, it, playerId) }
}