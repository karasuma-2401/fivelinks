package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.TacticalEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

class AiFacadeService(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
    private val tactical: TacticalEngine = TacticalEngine(heuristic)
): AiService {
    override suspend fun chooseMove(
        state: GameState,
        playerId: PlayerId,
        difficulty: Difficulty
    ): Move {
        val config = DifficultyConfig.from(difficulty)
        val legal = GameEngine.legalMoves(state,playerId)
        require(legal.isNotEmpty()) { "No legal moves for $playerId"}

        // setup later & config more and more
        if (config.useTacticalForced) {
            tactical.findForceMove(state, playerId)?.let { return it }
        }

        return heuristic.chooseMove(state, playerId, difficulty)
    }
}