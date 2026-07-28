package com.karasuma.fivelinks.fivelinks_cmp.ai

import kotlin.test.Test
import kotlin.test.assertEquals

class DifficultyConfigTest {
    @Test
    fun hardUsesHybridEvalMode() {
        assertEquals(EvalMode.Hybrid, DifficultyConfig.from(Difficulty.HARD).evalMode)
    }

    @Test
    fun easyAndMediumStayHeuristicOnly() {
        assertEquals(EvalMode.HeuristicOnly, DifficultyConfig.from(Difficulty.EASY).evalMode)
        assertEquals(EvalMode.HeuristicOnly, DifficultyConfig.from(Difficulty.MEDIUM).evalMode)
    }
}
