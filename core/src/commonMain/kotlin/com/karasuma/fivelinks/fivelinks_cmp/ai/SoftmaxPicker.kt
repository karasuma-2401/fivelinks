package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import kotlin.math.exp
import kotlin.random.Random

object SoftmaxPicker {
    fun pick (
        scored: List<Pair<Move, Double>>,
        temperature: Double,
        random: Random = Random.Default,
        scoreScale: Double = 1_000.0
    ): Move {
        require(scored.isNotEmpty()) {"empty pool" }
        if (scored.size == 1 || temperature <= 0.0)
            return scored.first().first
        val maxScore = scored.maxOf { it.second }
        val weights = scored.map { (_, s) ->
            exp((s-maxScore) / (scoreScale * temperature))
        }
        val total = weights.sum()
        var r = random.nextDouble() * total
        for (i in scored.indices) {
            r -= weights[i]
            if (r <= 0.0) return scored[i].first
        }
        return scored.last().first
    }
}