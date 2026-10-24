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
                neuralPriorsWithHeuristicFallback(
                    logits = out.policyLogits,
                    moves = moves,
                    codec = codec,
                    heuristicPriors = fallback.priors,
                )
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

/**
 * Combines the Classic neural policy with heuristic priors for Tactical actions which are not
 * part of [ActionCodec]'s action space yet. This keeps Craft Place/Remove and Divine Wipe on an
 * intentional, valid path instead of relying on an exception from `codec.encode`.
 */
internal fun neuralPriorsWithHeuristicFallback(
    logits: FloatArray,
    moves: List<Move>,
    codec: ActionCodec,
    heuristicPriors: FloatArray?,
): FloatArray {
    if (moves.isEmpty()) return floatArrayOf()

    val fallback = normalizedPriors(heuristicPriors, moves.size)
    val supported = moves.mapIndexedNotNull { index, move ->
        codec.encodeOrNull(move)?.let { actionIndex -> index to actionIndex }
    }
    if (supported.isEmpty() || supported.any { (_, actionIndex) -> actionIndex !in logits.indices }) return fallback

    val neuralLogits = supported.map { (_, actionIndex) -> logits[actionIndex] }
    val max = neuralLogits.max()
    if (!max.isFinite()) return fallback
    val neuralWeights = neuralLogits.map { logit -> exp((logit - max).toDouble()) }
    val neuralSum = neuralWeights.sum().coerceAtLeast(1e-12)
    val supportedIndices = supported.map { it.first }.toSet()
    val heuristicTacticalMass = fallback.indices
        .filterNot(supportedIndices::contains)
        .sumOf { fallback[it].toDouble() }
        .coerceIn(0.0, 1.0)
    val neuralMass = 1.0 - heuristicTacticalMass

    val merged = FloatArray(moves.size)
    fallback.indices.filterNot(supportedIndices::contains).forEach { index ->
        merged[index] = fallback[index]
    }
    supported.forEachIndexed { neuralIndex, (moveIndex, _) ->
        merged[moveIndex] = (neuralWeights[neuralIndex] / neuralSum * neuralMass).toFloat()
    }
    return normalizedPriors(merged, moves.size)
}

private fun normalizedPriors(priors: FloatArray?, size: Int): FloatArray {
    val source = priors?.takeIf { it.size == size } ?: FloatArray(size) { 1f / size }
    val sum = source.sumOf { value -> value.coerceAtLeast(0f).toDouble() }
    if (sum <= 1e-12) return FloatArray(size) { 1f / size }
    return FloatArray(size) { index -> (source[index].coerceAtLeast(0f) / sum).toFloat() }
}
