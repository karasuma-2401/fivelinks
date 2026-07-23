package com.karasuma.fivelinks.fivelinks_cmp.ai.tactical

import com.karasuma.fivelinks.fivelinks_cmp.ai.AiFacadeService
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.Board
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipMap
import com.karasuma.fivelinks.fivelinks_cmp.domain.Deck
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TacticalEngineTest {

    private val tactical = TacticalEngine()

    /** Row 0 cols 1..5 = 6D,7D,8D,9D,10D (no corners). */
    private val diamondLine: List<BoardPosition> = (1..5).map { BoardPosition(0, it) }

    private fun fixtureState(
        chips: Map<BoardPosition, Team>,
        handP0: List<Card>,
        playerCount: Int = 3,
        teamCount: Int = 3,
    ): GameState {
        val config = GameConfig.forPlayer(playerCount, teamCount, seed = 1L)
        val players = config.teamsByPlayerIndex.mapIndexed { index, team ->
            Player(id = "p$index", name = team.name, team = team, isAi = true)
        }
        val chipMap = ChipMap(chips.entries.associate { (pos, team) -> pos.flatIndex to team })
        val hands = players.associate { player ->
            player.id to Hand(if (player.id == "p0") handP0 else emptyList())
        }
        return GameState(
            config = config,
            players = players,
            board = Board.standard,
            chips = chipMap,
            hands = hands,
            deck = Deck(listOf(Card(Suit.CLUBS, Rank.TWO), Card(Suit.CLUBS, Rank.THREE))),
            discard = emptyList(),
            currentPlayerIndex = 0,
        )
    }

    @Test
    fun mustCompleteSequenceWhenAvailable() {
        val chips = diamondLine.take(4).associateWith { Team.RED }
        val finishPos = diamondLine[4]
        val finishCard = Board.standard.cardAt(finishPos)!!
        val state = fixtureState(chips, listOf(finishCard))

        assertEquals(1, state.config.sequenceToWin)

        val move = tactical.findForceMove(state, "p0")
        assertIs<Move.Place>(move)
        assertEquals(finishPos, move.position)
        assertEquals(finishCard, move.card)

        val after = GameEngine.applyMove(state, move).getOrThrow()
        assertEquals(Team.RED, after.winner)
        assertTrue(after.sequencesOf(Team.RED) >= 1)
    }

    @Test
    fun mustBlockOpponentOpenFour() {
        val chips = diamondLine.take(4).associateWith { Team.BLUE }
        val dangerPos = diamondLine[4]
        val blockCard = Board.standard.cardAt(dangerPos)!!
        val state = fixtureState(chips, listOf(blockCard))

        assertEquals(1, ThreatDetector.countOpenFours(state, Team.BLUE))
        assertEquals(setOf(dangerPos), ThreatDetector.openFoursEmptyCells(state, Team.BLUE))

        val move = tactical.findForceMove(state, "p0")
        assertIs<Move.Place>(move)
        assertEquals(dangerPos, move.position)
        assertEquals(blockCard, move.card)
    }

    @Test
    fun facade_usesTacticalForcedMove() = runTest {
        val chips = diamondLine.take(4).associateWith { Team.RED }
        val finishPos = diamondLine[4]
        val finishCard = Board.standard.cardAt(finishPos)!!
        val state = fixtureState(chips, listOf(finishCard))
        val facade = AiFacadeService()

        // EASY vẫn phải lấy forced win (useTacticalForced = true)
        val move = facade.chooseMove(state, "p0", Difficulty.EASY)
        assertIs<Move.Place>(move)
        assertEquals(finishPos, move.position)
    }
}
