package com.karasuma.fivelinks.fivelinks_cmp.ai.eval

import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.math.exp
import kotlin.math.tanh

class HeuristicPositionEvaluator(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator()
): IPositionEvaluator {
    override fun value (state: GameState, perspective: Team): Float {
        if (state.winner == perspective) return 1f
        if (state.winner != null) return -1f

        val player = state.players.firstOrNull { it.team == perspective && it.id == state.currentPlayer.id }
        val mySeq = state.sequencesOf(perspective)
        val opp = state.config.teams.filter { it != perspective }.sumOf { state.sequencesOf(it) }
        val raw = (mySeq - opp).toFloat()
        return tanh(raw.toDouble()).toFloat()
    }
}

class HeuristicPrior(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator()
): IPolicyPrior {
    override fun priors(state: GameState, playerId: PlayerId, moves: List<Move>): FloatArray {
        if (moves.isEmpty()) return floatArrayOf()
        val scores = moves.map { heuristic.score(state, it, playerId) }
        val max = scores.max()
        val exps = scores.map { exp((it - max) / 1000.0) }
        val sum = exps.sum()
        return FloatArray(moves.size) { i -> (exps[i] / sum).toFloat() }
    }
}

class HeuristicEvaluationEngine(
    private val values: IPositionEvaluator = HeuristicPositionEvaluator(),
    private val policy: IPolicyPrior = HeuristicPrior()
): IEvaluationEngine {
    override fun evaluate(
        state: GameState,
        perspective: Team,
        playerId: PlayerId,
        moves: List<Move>,
        wantPriors: Boolean
    ): EvalResult = EvalResult(
        value = values.value(state, perspective),
        priors = if (wantPriors) policy.priors(state, playerId, moves) else null
    )
}