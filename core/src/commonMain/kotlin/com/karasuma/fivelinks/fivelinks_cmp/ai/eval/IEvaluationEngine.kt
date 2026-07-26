package com.karasuma.fivelinks.fivelinks_cmp.ai.eval

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team

fun interface IPositionEvaluator {
    fun value(state: GameState, perspective: Team): Float
}

fun interface IPolicyPrior {
    fun priors(state: GameState, playerId: PlayerId, moves: List<Move>): FloatArray
}
data class EvalResult(
    val value: Float,
    val priors: FloatArray?
)

fun interface IEvaluationEngine {
    fun evaluate(
        state: GameState,
        perspective: Team,
        playerId: PlayerId,
        moves: List<Move>,
        wantPriors: Boolean
    ): EvalResult
}
