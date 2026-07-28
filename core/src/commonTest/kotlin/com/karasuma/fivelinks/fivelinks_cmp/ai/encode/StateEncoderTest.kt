package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StateEncoderTest {

    private val encoder = StateEncoder()

    @Test
    fun encode_outputHasCorrectSize() {
        val state = initState(12L)
        val tensor = encoder.encode(state, Team.RED)

        assertEquals(encoder.size, tensor.size)
        assertEquals(StateEncoder.CHANNELS * 10 * 10, tensor.size)
        assertEquals(1200, tensor.size)
    }

    @Test
    fun encode_initialState_channelsArePlausible() {
        val state = initState(12L)
        val tensor = encoder.encode(state, Team.RED)

        fun idx(c: Int, r: Int, col: Int) = c * 100 + r * 10 + col
        fun channelSum(c: Int) = (0..99).sumOf { tensor[idx(c, it / 10, it % 10)].toDouble() }

        assertEquals(0.0, channelSum(0))
        assertEquals(0.0, channelSum(1))
        assertEquals(96.0, channelSum(2))
        assertEquals(4.0, channelSum(3))
        assertEquals(0.0, channelSum(4))
        assertTrue(channelSum(5) > 0.0, "hand playable should mark some cells")
        assertEquals(0.0, channelSum(6))
        assertEquals(0.0, channelSum(7))
        assertEquals(0.0, channelSum(8))
        assertEquals(100.0, channelSum(9), absoluteTolerance = 1e-4) // RED needs full toWin → 1.0 * 100
        assertEquals(100.0, channelSum(10), absoluteTolerance = 1e-4)
        assertTrue(channelSum(11) in 1.0..100.0, "deck norm should be positive after deal")
    }

    @Test
    fun encode_differentSeeds_produceDifferentTensors() {
        val a = encoder.encode(initState(1L), Team.RED)
        val b = encoder.encode(initState(2L), Team.RED)
        assertNotEquals(a.toList(), b.toList())
    }

    private fun initState(seed: Long) = GameEngine.initialize(
        GameConfig.forPlayer(2, 2, seed),
        listOf(
            Player("p0", "Player 1", Team.RED, isAi = false),
            Player("p1", "Player 2", Team.BLUE, isAi = true),
        ),
    )
}
