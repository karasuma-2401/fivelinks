package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiService
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @Test
    fun facadeFailure_fallsBackToAValidHeuristicMoveWithoutBlockingTheUi() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val failingAi = object : AiService {
                var calls = 0

                override suspend fun chooseMove(state: GameState, playerId: PlayerId, difficulty: Difficulty): Move {
                    calls++
                    error("primary engine unavailable")
                }
            }
            val viewModel = aiFirstViewModel(failingAi, dispatcher)

            advanceUntilIdle()

            assertEquals(1, failingAi.calls)
            assertEquals(1, viewModel.uiState.value.gameState.turnNumber)
            assertFalse(viewModel.uiState.value.isAiThinking)
            viewModel.close()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restartingOrLeaving_cancelsTheCurrentAiSearch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val slowAi = CancellableAi()
            val viewModel = aiFirstViewModel(slowAi, dispatcher)
            advanceUntilIdle()
            assertEquals(1, slowAi.started)
            assertTrue(viewModel.uiState.value.isAiThinking)

            viewModel.startNewGame()
            advanceUntilIdle()
            assertTrue(slowAi.cancelled >= 1)
            assertEquals(2, slowAi.started)

            viewModel.close()
            advanceUntilIdle()
            assertTrue(slowAi.cancelled >= 2)
            assertFalse(viewModel.uiState.value.isAiThinking)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun aiFirstViewModel(ai: AiService, dispatcher: CoroutineDispatcher): GameViewModel =
        GameViewModel(
            initialConfig = GameConfig.soloVsAi(seed = 9L),
            initialPlayers = listOf(
                Player("p0", "AI", Team.BLUE, isAi = true),
                Player("p1", "Human", Team.RED, isAi = false),
            ),
            aiService = ai,
            aiDispatcher = dispatcher,
            aiThinkDelayMillis = 0,
            passDelayMillis = 0,
        )

    private class CancellableAi : AiService {
        var started = 0
        var cancelled = 0

        override suspend fun chooseMove(state: GameState, playerId: PlayerId, difficulty: Difficulty): Move {
            started++
            try {
                awaitCancellation()
            } finally {
                cancelled++
            }
        }
    }
}
