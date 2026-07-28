package com.karasuma.fivelinks.fivelinks_cmp.ai.inference

import com.karasuma.fivelinks.fivelinks_cmp.ai.encode.StateEncoder
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs only when `models/model_v1.onnx` is on the JVM classpath (after export_onnx).
 */
class OnnxNeuralInferenceTest {

    @Test
    fun infer_whenModelPresent_returnsPolicyAndValue() = runTest {
        val nn = createNeuralInferenceOrNull()
        if (nn == null) {
            println("Skip ONNX infer test (no models/model_v1.onnx on classpath)")
            return@runTest
        }
        assertTrue(nn.isAvailable)

        val config = GameConfig.soloVsAi(seed = 1L)
        val players = listOf(
            Player("p0", "A", Team.RED, isAi = true),
            Player("p1", "B", Team.BLUE, isAi = true),
        )
        val state = GameEngine.initialize(config, players)
        val tensor = StateEncoder().encode(state, state.currentPlayer.team)
        val out = nn.infer(tensor)
        assertTrue(out.policyLogits.isNotEmpty())
        assertTrue(out.value in -1.01f..1.01f)
    }
}
