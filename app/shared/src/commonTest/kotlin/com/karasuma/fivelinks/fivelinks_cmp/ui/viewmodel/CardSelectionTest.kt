package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardSelectionTest {

    private val blue = Player("p0", "Blue", Team.BLUE)
    private val red = Player("p1", "Red", Team.RED)

    private fun game(vararg hand: Card, tactical: Boolean = true): GameState {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = 1)
            .copy(enableTacticalCrafting = tactical)
        val dealt = GameEngine.initialize(config, listOf(blue, red))
        return dealt.copy(hands = mapOf(blue.id to Hand(hand.toList()), red.id to Hand(emptyList())))
    }

    private fun GameState.withChips(team: Team, vararg positions: BoardPosition): GameState =
        copy(chips = positions.fold(chips) { chips, position -> chips.place(position, team) })

    private fun openCells(state: GameState): Set<BoardPosition> =
        BoardPosition.all.filter { !state.board.isCorner(it) && !state.chips.contains(it) }.toSet()

    private val sevenOfHearts = Card(Suit.HEARTS, Rank.SEVEN)

    @Test
    fun aPlainCard_pointsAtItsOpenCells() {
        val cells = game(sevenOfHearts).board.positionsOf(sevenOfHearts)
        val state = game(sevenOfHearts).withChips(Team.RED, cells[0])

        val selection = cardSelection(state, blue, setOf(0))

        assertNull(selection.tacticalAction)
        assertEquals(setOf(cells[1]), selection.placements)
        assertTrue(selection.snipes.isEmpty())
        assertEquals(Move.Place(blue.id, sevenOfHearts, cells[1]), moveForTap(state, blue, selection, cells[1]))
        assertNull(moveForTap(state, blue, selection, cells[0]))
    }

    @Test
    fun aTwoEyedJack_goesOnAnyOpenCell() {
        val jack = Card(Suit.DIAMONDS, Rank.JACK)
        val taken = BoardPosition(0, 1)
        val state = game(jack).withChips(Team.RED, taken)

        val selection = cardSelection(state, blue, setOf(0))

        assertEquals(openCells(state), selection.placements)
        assertEquals(Move.Place(blue.id, jack, BoardPosition(5, 5)), moveForTap(state, blue, selection, BoardPosition(5, 5)))
        assertNull(moveForTap(state, blue, selection, taken))
    }

    @Test
    fun aOneEyedJack_targetsOpponentChips() {
        val jack = Card(Suit.SPADES, Rank.JACK)
        val theirs = BoardPosition(2, 2)
        val mine = BoardPosition(3, 3)
        val state = game(jack).withChips(Team.RED, theirs).withChips(Team.BLUE, mine)

        val selection = cardSelection(state, blue, setOf(0))

        assertEquals(setOf(theirs), selection.snipes)
        assertTrue(selection.placements.isEmpty())
        assertEquals(Move.Remove(blue.id, jack, theirs), moveForTap(state, blue, selection, theirs))
        assertNull(moveForTap(state, blue, selection, mine))
    }

    @Test
    fun aPair_placesAnywhereOpen() {
        val other = Card(Suit.SPADES, Rank.SEVEN)
        val state = game(sevenOfHearts, other)

        val selection = cardSelection(state, blue, setOf(0, 1))

        assertEquals(TacticalAction.PairWild(sevenOfHearts, other), selection.tacticalAction)
        assertEquals(openCells(state), selection.placements)
        val target = BoardPosition(4, 4)
        assertEquals(Move.CraftPlace(blue.id, sevenOfHearts, other, target), moveForTap(state, blue, selection, target))
    }

    @Test
    fun suitedConnectors_snipeAChip() {
        val eight = Card(Suit.DIAMONDS, Rank.EIGHT)
        val nine = Card(Suit.DIAMONDS, Rank.NINE)
        val theirs = BoardPosition(6, 6)
        val state = game(eight, nine).withChips(Team.RED, theirs)

        val selection = cardSelection(state, blue, setOf(0, 1))

        assertEquals(TacticalAction.ConnectorSnipe(eight, nine), selection.tacticalAction)
        assertEquals(setOf(theirs), selection.snipes)
        assertEquals(Move.CraftRemove(blue.id, eight, nine, theirs), moveForTap(state, blue, selection, theirs))
    }

    @Test
    fun aStraightFlush_offersTheDivineWipe() {
        val flush = listOf(Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE, Rank.SIX).map { Card(Suit.CLUBS, it) }
        val theirs = BoardPosition(1, 1)
        val state = game(*flush.toTypedArray()).withChips(Team.RED, theirs)

        val selection = cardSelection(state, blue, (0..4).toSet())

        assertEquals(TacticalAction.DivineWipe(flush), selection.tacticalAction)
        assertEquals(setOf(theirs), selection.snipes)
        // The wipe is triggered by its own button, not by tapping the board.
        assertNull(moveForTap(state, blue, selection, theirs))
    }

    @Test
    fun withoutTacticalCrafting_aPairDoesNothing() {
        val state = game(sevenOfHearts, Card(Suit.SPADES, Rank.SEVEN), tactical = false)

        val selection = cardSelection(state, blue, setOf(0, 1))

        assertNull(selection.tacticalAction)
        assertTrue(selection.placements.isEmpty() && selection.snipes.isEmpty())
    }
}
