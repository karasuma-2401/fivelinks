package com.karasuma.fivelinks.fivelinks_cmp.ai

import kotlin.test.Test
import kotlin.test.assertEquals

class DifficultyConfigTest {

    @Test
    fun `from EASY maps to correct config`() {
        val config = DifficultyConfig.from(Difficulty.EASY)
        assertEquals(80, config.maxSimulations)
        assertEquals(0.9, config.temperature)
        assertEquals(EvalMode.HeuristicOnly, config.evalMode)
    }

    @Test
    fun `from MEDIUM maps to correct config`() {
        val config = DifficultyConfig.from(Difficulty.MEDIUM)
        assertEquals(400, config.maxSimulations)
        assertEquals(0.35, config.temperature)
        assertEquals(EvalMode.HeuristicOnly, config.evalMode)
    }

    @Test
    fun `from HARD maps to correct config`() {
        val config = DifficultyConfig.from(Difficulty.HARD)
        assertEquals(2000, config.maxSimulations)
        assertEquals(0.0, config.temperature)
        assertEquals(EvalMode.Hybrid, config.evalMode)
    }
}