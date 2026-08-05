package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.ai.search.MoveKey
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
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

class AiRegressionTest {

    private val ai = AiFacadeService()

    @Test
    fun `mustWin_whenOneMoveAway`() = runTest {
        val state = createMustWinState()
        val expectedMove = Move.Place("p0", Card(Suit.SPADES, Rank.SEVEN), BoardPosition(5, 5))

        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)

        assertEquals(MoveKey.from(expectedMove), MoveKey.from(chosenMove))
    }

    @Test
    fun `mustBlock_whenOpponentHasOpenFour`() = runTest {
        val state = createMustBlockState()
        // The only way to prevent BLUE from winning is to place a chip at (3, 5)
        val expectedMove = Move.Place("p0", Card(Suit.DIAMONDS, Rank.FIVE), BoardPosition(3, 5))

        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)

        assertEquals(MoveKey.from(expectedMove), MoveKey.from(chosenMove))
    }

    private fun createMustWinState(): GameState {
        val config = GameConfig.forPlayer(2, 2, seed = 42)
        val players = listOf(
            Player("p0", "Red", Team.RED, true),
            Player("p1", "Blue", Team.BLUE, true)
        )
        val initialState = GameEngine.initialize(config, players)

        val chips = initialState.chips
            .place(BoardPosition(5, 1), Team.RED)
            .place(BoardPosition(5, 2), Team.RED)
            .place(BoardPosition(5, 3), Team.RED)
            .place(BoardPosition(5, 4), Team.RED)

        return initialState.copy(
            hands = mapOf(
                "p0" to Hand(listOf(Card(Suit.SPADES, Rank.SEVEN), Card(Suit.SPADES, Rank.EIGHT))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.TWO), Card(Suit.HEARTS, Rank.THREE)))
            ),
            chips = chips,
            currentPlayerIndex = 0
        )
    }

    private fun createMustBlockState(): GameState {
        val config = GameConfig.forPlayer(2, 2, seed = 43)
        val players = listOf(
            Player("p0", "Red", Team.RED, true),
            Player("p1", "Blue", Team.BLUE, true)
        )
        val initialState = GameEngine.initialize(config, players)

        // Set up a board where BLUE has an open-four at row 3
        val chips = initialState.chips
            .place(BoardPosition(3, 1), Team.BLUE)
            .place(BoardPosition(3, 2), Team.BLUE)
            .place(BoardPosition(3, 3), Team.BLUE)
            .place(BoardPosition(3, 4), Team.BLUE)

        return initialState.copy(
            hands = mapOf(
                // Player "p0" has the card needed to block at (3, 5)
                "p0" to Hand(listOf(Card(Suit.DIAMONDS, Rank.FIVE), Card(Suit.CLUBS, Rank.ACE))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.SPADES, Rank.QUEEN)))
            ),
            chips = chips,
            currentPlayerIndex = 0 // It's RED's turn to block
        )
    }
}