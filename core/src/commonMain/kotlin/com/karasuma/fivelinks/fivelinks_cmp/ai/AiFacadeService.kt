package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.eval.HeuristicEvaluationEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.SearchEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.TacticalEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlin.random.Random

class AiFacadeService(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
    private val tactical: TacticalEngine = TacticalEngine(heuristic),
    private val search: SearchEngine = SearchEngine(HeuristicEvaluationEngine(), tactical),
    private val random: Random = Random.Default
): AiService {
    override suspend fun chooseMove(
        state: GameState,
        playerId: PlayerId,
        difficulty: Difficulty
    ): Move {
        val config = DifficultyConfig.from(difficulty)
        val legal = GameEngine.legalMoves(state,playerId)
        require(legal.isNotEmpty()) { "No legal moves for $playerId"}

        if (config.useTacticalForced) {
            tactical.findForceMove(state, playerId)?.let { return it }
        }

        val visits = search.search(state, playerId, config)
        if (visits.isEmpty()) {
            return heuristic.chooseMove(state, playerId, difficulty)
        }
        return selectByVisits(visits, playerId, config, random)
    }
}