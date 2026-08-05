package com.karasuma.fivelinks.fivelinks_cmp.ai.debug

import com.karasuma.fivelinks.fivelinks_cmp.ai.EvalMode
import com.karasuma.fivelinks.fivelinks_cmp.ai.search.MoveKey

data class AiDebugInfo(
    val tacticalHit: Boolean,
    val simulationsRun: Int,
    val determinizations: Int,
    val chosenKey: MoveKey,
    val evalMode: EvalMode,
)
