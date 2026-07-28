package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.search.MoveKey
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.toMove
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlin.random.Random

internal fun selectByVisits(
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
            scoreScale = 1.0,
        )
    }
}
