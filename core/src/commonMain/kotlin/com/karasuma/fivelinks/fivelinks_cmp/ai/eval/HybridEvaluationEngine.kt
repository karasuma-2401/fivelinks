package com.karasuma.fivelinks.fivelinks_cmp.ai.eval

import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.ActionCodec
import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.StateEncoder
import com.karasuma.fivelinks.fivelinks_cmp.ai.inference.NeuralInference
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.math.exp

class HybridEvaluationEngine(
    private val heuristic: HeuristicEvaluationEngine = HeuristicEvaluationEngine(),
    private val encoder: StateEncoder = StateEncoder(),
    private val codec: ActionCodec = ActionCodec(),
    private val neural: NeuralInference?,
) : IEvaluationEngine {

    override suspend fun evaluate(
        state: GameState,
        perspective: Team,
        playerId: PlayerId,
        moves: List<Move>,
        wantPriors: Boolean,
    ): EvalResult {
        val fallback = heuristic.evaluate(state, perspective, playerId, moves, wantPriors)
        val nn = neural
        if (nn == null || !nn.isAvailable || nn.encoderVersion != encoder.version) {
            return fallback
        }
        return try {
            val tensor = encoder.encode(state, perspective)
            val out = nn.infer(tensor)
            val priors = if (wantPriors && moves.isNotEmpty()) {
                priorsFromLogits(out.policyLogits, moves, codec)
            } else {
                null
            }
            EvalResult(value = out.value, priors = priors)
        } catch (_: Throwable) {
            fallback
        }
    }
}

fun priorsFromLogits(
    logits: FloatArray,
    moves: List<Move>,
    codec: ActionCodec,
): FloatArray {
    if (moves.isEmpty()) return floatArrayOf()
    val idxs = moves.map { codec.encode(it) }
    val max = idxs.maxOf { logits[it] }
    val exps = idxs.map { exp((logits[it] - max).toDouble()) }
    val sum = exps.sum().coerceAtLeast(1e-12)
    return FloatArray(moves.size) { (exps[it] / sum).toFloat() }
}
