package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StuckTurnTest {

    private fun twoPlayers(): GameState = GameEngine.initialize(
        GameConfig.soloVsAi(seed = 3L),
        listOf(
            Player(id = "p0", name = "Human", team = Team.RED, isAi = false),
            Player(id = "p1", name = "AI", team = Team.BLUE, isAi = true),
        )
    )

    /** A card that is not a jack, so it can only be played on its own two cells. */
    private val plainCard = Card(Suit.HEARTS, Rank.SEVEN)

    @Test
    fun playableTurn_isLeftAlone() {
        val state = twoPlayers()

        val skipped = GameEngine.skipStuckTurns(state)

        assertTrue(skipped.passed.isEmpty())
        assertSame(state, skipped.state)
    }

    @Test
    fun emptyHandAndEmptyDeck_passesToTheOpponent() {
        val state = twoPlayers().copy(
            deck = Deck(emptyList()),
            hands = mapOf("p0" to Hand(emptyList()), "p1" to Hand(listOf(plainCard))),
        )

        val skipped = GameEngine.skipStuckTurns(state)

        assertEquals(listOf("p0"), skipped.passed)
        assertEquals("p1", skipped.state.currentPlayer.id)
        assertEquals(state.turnNumber + 1, skipped.state.turnNumber)
        assertFalse(skipped.state.isGameOver)
    }

    @Test
    fun deadCardWithEmptyDeck_cannotBeSwapped_soThePlayerPasses() {
        val base = twoPlayers()
        // Both cells of the card are taken and there is nothing left to draw.
        var chips = base.chips
        for (position in base.board.positionsOf(plainCard)) chips = chips.place(position, Team.BLUE)
        val state = base.copy(
            chips = chips,
            deck = Deck(emptyList()),
            hands = mapOf("p0" to Hand(listOf(plainCard)), "p1" to Hand(listOf(Card(Suit.CLUBS, Rank.TWO)))),
        )

        val skipped = GameEngine.skipStuckTurns(state)

        assertEquals(listOf("p0"), skipped.passed)
        assertEquals("p1", skipped.state.currentPlayer.id)
    }

    @Test
    fun nobodyCanMove_endsInADraw() {
        val state = twoPlayers().copy(
            deck = Deck(emptyList()),
            hands = mapOf("p0" to Hand(emptyList()), "p1" to Hand(emptyList())),
        )

        val skipped = GameEngine.skipStuckTurns(state)
        val drawn = skipped.state

        assertEquals(listOf("p0", "p1"), skipped.passed)
        assertTrue(drawn.isDraw)
        assertTrue(drawn.isGameOver)
        assertEquals(null, drawn.winner)
        assertTrue(GameEngine.legalMoves(drawn, drawn.currentPlayer.id).isEmpty())
        // A finished game accepts no more moves, and skipping again changes nothing.
        val anyMove = Move.Place("p0", plainCard, drawn.board.positionsOf(plainCard).first())
        assertTrue(GameEngine.applyMove(drawn, anyMove).isFailure)
        assertSame(drawn, GameEngine.skipStuckTurns(drawn).state)
    }
}
