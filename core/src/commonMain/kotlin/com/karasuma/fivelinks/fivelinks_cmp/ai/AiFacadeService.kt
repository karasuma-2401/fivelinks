package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.ActionCodec
import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.StateEncoder
import com.karasuma.fivelinks.fivelinks_cmp.ai.eval.HeuristicEvaluationEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.eval.HybridEvaluationEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.eval.IEvaluationEngine
import com.karasuma.fivelinks.fivelinks_cmp.ai.inference.NeuralInference
import com.karasuma.fivelinks.fivelinks_cmp.ai.inference.createNeuralInferenceOrNull
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
    private val neural: NeuralInference? = createNeuralInferenceOrNull(),
    private val random: Random = Random.Default,
) : AiService {
    private val heuristicEval = HeuristicEvaluationEngine()
    private val hybridEval = HybridEvaluationEngine(
        heuristic = heuristicEval,
        encoder = StateEncoder(),
        codec = ActionCodec(),
        neural = neural,
    )
    private val searchEngine = SearchEngine(tactical)

    override suspend fun chooseMove(
        state: GameState,
        playerId: PlayerId,
        difficulty: Difficulty,
    ): Move {
        val config = DifficultyConfig.from(difficulty)
        val legal = GameEngine.legalMoves(state, playerId)
        require(legal.isNotEmpty()) { "No legal moves for $playerId" }

        if (config.useTacticalForced) {
            tactical.findForceMove(state, playerId)?.let { return it }
        }

        val eval = evaluationFor(config.evalMode)
        val visits = searchEngine.search(state, playerId, config, eval)
        if (visits.isEmpty()) {
            return heuristic.chooseMove(state, playerId, difficulty)
        }
        return selectByVisits(visits, playerId, config, random)
    }

    private fun evaluationFor(mode: EvalMode): IEvaluationEngine = when (mode) {
        EvalMode.HeuristicOnly -> heuristicEval
        EvalMode.Hybrid, EvalMode.NeuralFirst -> hybridEval
    }
}