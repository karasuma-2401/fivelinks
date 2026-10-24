package com.karasuma.fivelinks.fivelinks_cmp.ai.eval

import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.ActionCodec
import com.karasuma.fivelinks.fivelinks_cmp.ai.inference.NeuralInference
import com.karasuma.fivelinks.fivelinks_cmp.ai.inference.NeuralOutput
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HybridEvaluationEngineTest {

    @Test
    fun fallsBackWhenNeuralUnavailable() = runTest {
        val config = GameConfig.soloVsAi(seed = 7L)
        val players = listOf(
            Player("p0", "A", Team.RED, isAi = true),
            Player("p1", "B", Team.BLUE, isAi = true),
        )
        val state = GameEngine.initialize(config, players)
        val pid = state.currentPlayer.id
        val moves = GameEngine.legalMoves(state, pid)
        val heuristic = HeuristicEvaluationEngine()
        val hybrid = HybridEvaluationEngine(heuristic = heuristic, neural = null)

        val h = heuristic.evaluate(state, state.currentPlayer.team, pid, moves, wantPriors = true)
        val y = hybrid.evaluate(state, state.currentPlayer.team, pid, moves, wantPriors = true)
        assertEquals(h.value, y.value)
        assertNotNull(y.priors)
        assertEquals(h.priors!!.size, y.priors!!.size)
    }

    @Test
    fun usesNeuralWhenAvailable() = runTest {
        val config = GameConfig.soloVsAi(seed = 11L)
        val players = listOf(
            Player("p0", "A", Team.RED, isAi = true),
            Player("p1", "B", Team.BLUE, isAi = true),
        )
        val state = GameEngine.initialize(config, players)
        val pid = state.currentPlayer.id
        val moves = GameEngine.legalMoves(state, pid).take(5)
        val codec = ActionCodec()
        val fake = object : NeuralInference {
            override val encoderVersion: Int = 1
            override val isAvailable: Boolean = true
            override suspend fun infer(stateNchw: FloatArray): NeuralOutput {
                val logits = FloatArray(codec.maxActions) { -10f }
                moves.forEachIndexed { i, move ->
                    logits[codec.encode(move)] = i.toFloat()
                }
                return NeuralOutput(policyLogits = logits, value = 0.42f)
            }
        }
        val hybrid = HybridEvaluationEngine(codec = codec, neural = fake)
        val result = hybrid.evaluate(state, state.currentPlayer.team, pid, moves, wantPriors = true)
        assertEquals(0.42f, result.value, 1e-5f)
        assertNotNull(result.priors)
        assertEquals(moves.size, result.priors!!.size)
        assertTrue(abs(result.priors!!.sum() - 1.0) < 1e-4)
        // last move has highest logit → highest prior
        assertEquals(result.priors!!.max(), result.priors!!.last())
    }

    @Test
    fun fallsBackOnInferException() = runTest {
        val config = GameConfig.soloVsAi(seed = 3L)
        val players = listOf(
            Player("p0", "A", Team.RED, isAi = true),
            Player("p1", "B", Team.BLUE, isAi = true),
        )
        val state = GameEngine.initialize(config, players)
        val pid = state.currentPlayer.id
        val moves = GameEngine.legalMoves(state, pid)
        val heuristic = HeuristicEvaluationEngine()
        val broken = object : NeuralInference {
            override val encoderVersion: Int = 1
            override val isAvailable: Boolean = true
            override suspend fun infer(stateNchw: FloatArray): NeuralOutput {
                error("boom")
            }
        }
        val hybrid = HybridEvaluationEngine(heuristic = heuristic, neural = broken)
        val h = heuristic.evaluate(state, state.currentPlayer.team, pid, moves, wantPriors = false)
        val y = hybrid.evaluate(state, state.currentPlayer.team, pid, moves, wantPriors = false)
        assertEquals(h.value, y.value)
    }

    @Test
    fun tacticalMoves_keepHeuristicPriorsInsteadOfThrowingFromTheClassicCodec() {
        val codec = ActionCodec()
        val normal = Move.Place("p0", Card(Suit.HEARTS, Rank.FIVE), BoardPosition(3, 4))
        val craft = Move.CraftPlace(
            playerId = "p0",
            card1 = Card(Suit.CLUBS, Rank.FOUR),
            card2 = Card(Suit.CLUBS, Rank.FIVE),
            position = BoardPosition(4, 4),
        )
        val wipe = Move.DivineWipe(
            playerId = "p0",
            cards = listOf(Card(Suit.SPADES, Rank.TEN), Card(Suit.SPADES, Rank.JACK)),
        )
        val logits = FloatArray(codec.maxActions) { -10f }.also {
            it[codec.encode(normal)] = 8f
        }

        val priors = neuralPriorsWithHeuristicFallback(
            logits = logits,
            moves = listOf(normal, craft, wipe),
            codec = codec,
            heuristicPriors = floatArrayOf(0.2f, 0.3f, 0.5f),
        )

        assertEquals(3, priors.size)
        assertEquals(0.3f, priors[1], 1e-5f)
        assertEquals(0.5f, priors[2], 1e-5f)
        assertEquals(1f, priors.sum(), 1e-5f)
    }
}
