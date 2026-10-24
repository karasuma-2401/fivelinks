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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AiRegressionTest {

    private val ai = AiFacadeService()

    @Test
    fun `mustWin_whenOneMoveAway`() = runTest {
        val state = createMustWinState()
        val expectedMove = Move.Place("p0", Card(Suit.DIAMONDS, Rank.FIVE), BoardPosition(7, 4))
        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)
        assertEquals(MoveKey.from(expectedMove), MoveKey.from(chosenMove))
    }

    @Test
    fun `mustBlock_whenOpponentHasOpenFour`() = runTest {
        val state = createMustBlockState()
        val expectedMove = Move.Place("p0", Card(Suit.DIAMONDS, Rank.FIVE), BoardPosition(7, 4))
        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)
        assertEquals(MoveKey.from(expectedMove), MoveKey.from(chosenMove))
    }

    @Test
    fun `mustUseJack_toRemoveThreat`() = runTest {
        val state = createMustUseJackState()
        // The only way to survive is to use the One-Eyed Jack to remove a blue chip
        val expectedMove = Move.Remove("p0", Card(Suit.SPADES, Rank.JACK), BoardPosition(2, 2))
        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)
        assertEquals(MoveKey.from(expectedMove), MoveKey.from(chosenMove))
    }

    @Test
    fun `tactical_mustWinWithCraftPlace_whenPairCompletesSequence`() = runTest {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = 43).copy(sequenceToWin = 1)
        val players = listOf(Player("p0", "Red", Team.RED, true), Player("p1", "Blue", Team.BLUE, true))
        val initialState = GameEngine.initialize(config, players)
        val chips = initialState.chips
            .place(BoardPosition(7, 0), Team.RED)
            .place(BoardPosition(7, 1), Team.RED)
            .place(BoardPosition(7, 2), Team.RED)
            .place(BoardPosition(7, 3), Team.RED)
        val state = initialState.copy(
            hands = mapOf(
                "p0" to Hand(listOf(Card(Suit.CLUBS, Rank.FOUR), Card(Suit.SPADES, Rank.FOUR))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.SPADES, Rank.QUEEN))),
            ),
            chips = chips,
            currentPlayerIndex = 0,
        )

        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)
        val craftPlace = assertIs<Move.CraftPlace>(chosenMove)
        assertEquals(BoardPosition(7, 4), craftPlace.position)
    }

    @Test
    fun `tactical_mustBlockWithCraftRemove_whenSuitedConnectorsBreakThreat`() = runTest {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = 44).copy(sequenceToWin = 1)
        val players = listOf(Player("p0", "Red", Team.RED, true), Player("p1", "Blue", Team.BLUE, true))
        val initialState = GameEngine.initialize(config, players)
        val chips = initialState.chips
            .place(BoardPosition(2, 1), Team.BLUE)
            .place(BoardPosition(2, 2), Team.BLUE)
            .place(BoardPosition(2, 3), Team.BLUE)
            .place(BoardPosition(2, 4), Team.BLUE)
            .place(BoardPosition(2, 5), Team.RED)
        val state = initialState.copy(
            hands = mapOf(
                "p0" to Hand(listOf(Card(Suit.CLUBS, Rank.FOUR), Card(Suit.CLUBS, Rank.FIVE))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN))),
            ),
            chips = chips,
            currentPlayerIndex = 0,
        )

        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)
        val craftRemove = assertIs<Move.CraftRemove>(chosenMove)
        assertEquals(BoardPosition(2, 2), craftRemove.position)
    }

    @Test
    fun `tactical_hybridSearch_choosesLegalMoveWhenCraftingAndDivineWipeAreAvailable`() = runTest {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = 45)
        val players = listOf(Player("p0", "Red", Team.RED, true), Player("p1", "Blue", Team.BLUE, true))
        val initialState = GameEngine.initialize(config, players)
        val state = initialState.copy(
            hands = mapOf(
                "p0" to Hand(
                    listOf(
                        Card(Suit.SPADES, Rank.FOUR),
                        Card(Suit.SPADES, Rank.FIVE),
                        Card(Suit.SPADES, Rank.SIX),
                        Card(Suit.HEARTS, Rank.FOUR),
                    )
                ),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN))),
            ),
            chips = initialState.chips.place(BoardPosition(4, 4), Team.BLUE),
            currentPlayerIndex = 0,
        )

        val chosenMove = ai.chooseMove(state, "p0", Difficulty.HARD)
        assertTrue(GameEngine.applyMove(state, chosenMove).isSuccess)
    }

    private fun createMustWinState(): GameState {
        val config = GameConfig.forPlayer(2, 2, seed = 43)
        val players = listOf(Player("p0", "Red", Team.RED, true), Player("p1", "Blue", Team.BLUE, true))
        val initialState = GameEngine.initialize(config, players)
        val chips = initialState.chips
            .place(BoardPosition(7, 0), Team.RED)
            .place(BoardPosition(7, 1), Team.RED)
            .place(BoardPosition(7, 2), Team.RED)
            .place(BoardPosition(7, 3), Team.RED)
        return initialState.copy(
            hands = mapOf(
                "p0" to Hand(listOf(Card(Suit.DIAMONDS, Rank.FIVE), Card(Suit.CLUBS, Rank.ACE))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.SPADES, Rank.QUEEN)))
            ),
            chips = chips,
            currentPlayerIndex = 0
        )
    }

    private fun createMustBlockState(): GameState {
        val config = GameConfig.forPlayer(2, 2, seed = 43)
        val players = listOf(Player("p0", "Red", Team.RED, true), Player("p1", "Blue", Team.BLUE, true))
        val initialState = GameEngine.initialize(config, players)
        val chips = initialState.chips
            .place(BoardPosition(7, 0), Team.BLUE)
            .place(BoardPosition(7, 1), Team.BLUE)
            .place(BoardPosition(7, 2), Team.BLUE)
            .place(BoardPosition(7, 3), Team.BLUE)
        return initialState.copy(
            hands = mapOf(
                "p0" to Hand(listOf(Card(Suit.DIAMONDS, Rank.FIVE), Card(Suit.CLUBS, Rank.ACE))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.SPADES, Rank.QUEEN)))
            ),
            chips = chips,
            currentPlayerIndex = 0
        )
    }

    private fun createMustUseJackState(): GameState {
        val config = GameConfig.forPlayer(2, 2, seed = 44)
        val players = listOf(Player("p0", "Red", Team.RED, true), Player("p1", "Blue", Team.BLUE, true))
        val initialState = GameEngine.initialize(config, players)

        // Blue has a line of 4, but the 5th spot is blocked by a RED chip.
        // This is a "dead" threat that can't be blocked by placing.
        val chips = initialState.chips
            .place(BoardPosition(2, 1), Team.BLUE)
            .place(BoardPosition(2, 2), Team.BLUE)
            .place(BoardPosition(2, 3), Team.BLUE)
            .place(BoardPosition(2, 4), Team.BLUE)
            .place(BoardPosition(2, 5), Team.RED) // Red chip blocking the line

        return initialState.copy(
            hands = mapOf(
                // Player "p0" has a One-Eyed Jack, the only tool to remove a blue chip.
                "p0" to Hand(listOf(Card(Suit.SPADES, Rank.JACK), Card(Suit.CLUBS, Rank.ACE))),
                "p1" to Hand(listOf(Card(Suit.HEARTS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN)))
            ),
            chips = chips,
            currentPlayerIndex = 0 // It's RED's turn
        )
    }
}
