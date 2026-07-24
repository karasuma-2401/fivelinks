package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.search.MoveKey
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.SearchEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.toMove
import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.TacticalEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlin.random.Random

class AiFacadeService(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
    private val tactical: TacticalEngine = TacticalEngine(heuristic),
    private val search: SearchEngine = SearchEngine(heuristic, tactical),
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

    private fun selectByVisits(
        visits: Map<MoveKey, Int>,
        playerId: PlayerId,
        config: DifficultyConfig,
        random: Random,
    ): Move {
        require(visits.isNotEmpty())
        val scored = visits.entries.map { (k, n) -> k.toMove(playerId) to n.toDouble() }
        return if (config.temperature <= 0.0 || config.topK <= 1) {
            scored.maxBy { it.second }.first
        } else {
            SoftmaxPicker.pick(
                scored.sortedByDescending { it.second }.take(config.topK),
                temperature = config.temperature,
                random = random,
                scoreScale = 1.0, // visit counts khác scale heuristic
            )
        }
    }
}