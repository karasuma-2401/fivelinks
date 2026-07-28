package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.ai.DifficultyConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SearchEngineTest {

    @Test
    fun search_returnsNonEmptyVisitCounts() = runTest {
        val config = GameConfig.soloVsAi(seed = 42L)
        val players = listOf(
            Player("p0", "AI_1", Team.RED, isAi = true),
            Player("p1", "AI_2", Team.BLUE, isAi = true),
        )
        val state = GameEngine.initialize(config, players)
        val playerId = state.currentPlayer.id
        val legal = GameEngine.legalMoves(state, playerId)
        assertTrue(legal.isNotEmpty())

        val visits = SearchEngine().search(state, playerId, DifficultyConfig.selfPlay())
        assertTrue(visits.isNotEmpty(), "MCTS must return at least one root child visit")
        assertTrue(visits.values.sum() > 0)
    }
}
