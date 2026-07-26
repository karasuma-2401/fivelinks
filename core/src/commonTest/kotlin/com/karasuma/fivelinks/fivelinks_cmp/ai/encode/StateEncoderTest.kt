package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.test.Test
import kotlin.test.assertEquals

class StateEncoderTest {

    private val encoder = StateEncoder()

    @Test
    fun encode_outputHasCorrectSize() {
        val config = GameConfig.forPlayer(2, 2, 12L)
        val players = listOf(
            Player("p0", "Player 1", Team.RED, isAi = false),
            Player("p1", "Player 2", Team.BLUE, isAi = true)
        )
        val state = GameEngine.initialize(config, players)
        
        val tensor = encoder.encode(state, Team.RED)
        
        assertEquals(encoder.size, tensor.size, "Encoded tensor size should match encoder.size")
        assertEquals(10 * 10 * 10, tensor.size, "Encoded tensor size should be 1000")
    }

    @Test
    fun encode_initialState_channelsArePlausible() {
        val config = GameConfig.forPlayer(2, 2, seed = 12L)
        val players = listOf(
            Player("p0", "Player 1", Team.RED, isAi = false),
            Player("p1", "Player 2", Team.BLUE, isAi = true)
        )
        val state = GameEngine.initialize(config, players)
        val tensor = encoder.encode(state, Team.RED)

        fun idx(c: Int, r: Int, col: Int) = c * 100 + r * 10 + col

        // Channel 0 (me_chips): Should be all 0s at the start
        val myChipsSum = (0..99).sumOf { tensor[idx(0, it / 10, it % 10)].toDouble() }
        assertEquals(0.0, myChipsSum)

        // Channel 1 (opp_chips): Should be all 0s at the start
        val oppChipsSum = (0..99).sumOf { tensor[idx(1, it / 10, it % 10)].toDouble() }
        assertEquals(0.0, oppChipsSum)

        // Channel 2 (empty): Should have 96 ones (100 cells - 4 corners)
        val emptySum = (0..99).sumOf { tensor[idx(2, it / 10, it % 10)].toDouble() }
        assertEquals(96.0, emptySum)

        // Channel 3 (corners): Should have 4 ones
        val cornersSum = (0..99).sumOf { tensor[idx(3, it / 10, it % 10)].toDouble() }
        assertEquals(4.0, cornersSum)

        // Channel 4 (locked): Should be all 0s at the start
        val lockedSum = (0..99).sumOf { tensor[idx(4, it / 10, it % 10)].toDouble() }
        assertEquals(0.0, lockedSum)
    }
}