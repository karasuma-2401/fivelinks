package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TacticalExpansionTest {

    private fun tacticalGame(seed: Long = 42L): GameState {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = seed)
        val players = listOf(
            Player(id = "p0", name = "Player1", team = Team.BLUE, isAi = false),
            Player(id = "p1", name = "Player2", team = Team.RED, isAi = true),
        )
        return GameEngine.initialize(config, players)
    }

    private fun classicGame(seed: Long = 42L): GameState {
        val config = GameConfig.soloVsAi(seed)
        val players = listOf(
            Player(id = "p0", name = "Player1", team = Team.BLUE, isAi = false),
            Player(id = "p1", name = "Player2", team = Team.RED, isAi = true),
        )
        return GameEngine.initialize(config, players)
    }

    // ==========================================
    // 1. TacticalPatterns Tests
    // ==========================================

    @Test
    fun testIsPair() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.SEVEN)
        val c3 = Card(Suit.SPADES, Rank.EIGHT)

        assertTrue(TacticalPatterns.isPair(c1, c2))
        assertFalse(TacticalPatterns.isPair(c1, c3))
    }

    @Test
    fun testIsSuitedConnector() {
        // Standard consecutive same suit
        val h8 = Card(Suit.HEARTS, Rank.EIGHT)
        val h9 = Card(Suit.HEARTS, Rank.NINE)
        assertTrue(TacticalPatterns.isSuitedConnector(h8, h9))
        assertTrue(TacticalPatterns.isSuitedConnector(h9, h8))

        // High cards: Queen & King
        val sQ = Card(Suit.SPADES, Rank.QUEEN)
        val sK = Card(Suit.SPADES, Rank.KING)
        assertTrue(TacticalPatterns.isSuitedConnector(sQ, sK))

        // Ace - Two wrap
        val cA = Card(Suit.CLUBS, Rank.ACE)
        val c2 = Card(Suit.CLUBS, Rank.TWO)
        assertTrue(TacticalPatterns.isSuitedConnector(cA, c2))
        assertTrue(TacticalPatterns.isSuitedConnector(c2, cA))

        // King - Ace
        val cK = Card(Suit.CLUBS, Rank.KING)
        assertTrue(TacticalPatterns.isSuitedConnector(cK, cA))

        // Different suits -> false
        val s8 = Card(Suit.SPADES, Rank.EIGHT)
        assertFalse(TacticalPatterns.isSuitedConnector(h8, s8))

        // Non-consecutive -> false
        val h10 = Card(Suit.HEARTS, Rank.TEN)
        assertFalse(TacticalPatterns.isSuitedConnector(h8, h10))
    }

    @Test
    fun testIsStraightFlush() {
        // Standard Straight Flush
        val sf1 = listOf(
            Card(Suit.DIAMONDS, Rank.FIVE),
            Card(Suit.DIAMONDS, Rank.SIX),
            Card(Suit.DIAMONDS, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.EIGHT),
            Card(Suit.DIAMONDS, Rank.NINE),
        )
        assertTrue(TacticalPatterns.isStraightFlush(sf1))

        // Royal Straight Flush (10 - J - Q - K - A)
        val royal = listOf(
            Card(Suit.SPADES, Rank.TEN),
            Card(Suit.SPADES, Rank.JACK),
            Card(Suit.SPADES, Rank.QUEEN),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.SPADES, Rank.ACE),
        )
        assertTrue(TacticalPatterns.isStraightFlush(royal))

        // Wheel Straight Flush (A - 2 - 3 - 4 - 5)
        val wheel = listOf(
            Card(Suit.CLUBS, Rank.ACE),
            Card(Suit.CLUBS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
            Card(Suit.CLUBS, Rank.FIVE),
        )
        assertTrue(TacticalPatterns.isStraightFlush(wheel))

        // Mixed suits -> false
        val mixedSuit = listOf(
            Card(Suit.DIAMONDS, Rank.FIVE),
            Card(Suit.HEARTS, Rank.SIX),
            Card(Suit.DIAMONDS, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.EIGHT),
            Card(Suit.DIAMONDS, Rank.NINE),
        )
        assertFalse(TacticalPatterns.isStraightFlush(mixedSuit))

        // Gap in ranks -> false
        val gap = listOf(
            Card(Suit.DIAMONDS, Rank.FIVE),
            Card(Suit.DIAMONDS, Rank.SIX),
            Card(Suit.DIAMONDS, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.NINE),
            Card(Suit.DIAMONDS, Rank.TEN),
        )
        assertFalse(TacticalPatterns.isStraightFlush(gap))

        // Wrong count -> false
        assertFalse(TacticalPatterns.isStraightFlush(sf1.take(4)))
    }

    @Test
    fun testFindPatternsInHand() {
        val hand = Hand(
            listOf(
                Card(Suit.HEARTS, Rank.EIGHT),
                Card(Suit.SPADES, Rank.EIGHT),
                Card(Suit.CLUBS, Rank.FIVE),
                Card(Suit.CLUBS, Rank.SIX),
                Card(Suit.CLUBS, Rank.SEVEN),
                Card(Suit.CLUBS, Rank.EIGHT),
                Card(Suit.CLUBS, Rank.NINE),
            )
        )

        // Find pairs: 8 of Hearts & 8 of Spades, or 8 of Clubs
        val pairs = TacticalPatterns.findPairs(hand)
        assertTrue(pairs.isNotEmpty())
        assertEquals(Rank.EIGHT, pairs.first().first.rank)
        assertEquals(Rank.EIGHT, pairs.first().second.rank)

        // Find suited connectors
        val connectors = TacticalPatterns.findSuitedConnectors(hand)
        assertTrue(connectors.isNotEmpty())

        // Find straight flush: 5-6-7-8-9 of Clubs
        val sf = TacticalPatterns.findStraightFlush(hand)
        assertNotNull(sf)
        assertEquals(5, sf.size)
        assertTrue(sf.all { it.suit == Suit.CLUBS })
    }

    // ==========================================
    // 2. CraftPlace Tests (Pair -> Wild Place)
    // ==========================================

    @Test
    fun testCraftPlace_successfulExecution() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.SEVEN)
        val targetPos = BoardPosition(3, 4)

        var state = tacticalGame()
        val p0 = state.currentPlayer
        // Set up player's hand with pair of 7s
        val customHand = Hand(listOf(c1, c2, Card(Suit.CLUBS, Rank.TWO), Card(Suit.DIAMONDS, Rank.THREE)))
        state = state.copy(hands = state.hands + (p0.id to customHand))

        val move = Move.CraftPlace(p0.id, c1, c2, targetPos)
        val next = GameEngine.applyMove(state, move).getOrThrow()

        // 1. Chip placed at targetPos with player's team
        assertEquals(Team.BLUE, next.chips.at(targetPos))

        // 2. Pair removed from hand, 2 cards drawn
        val nextHand = next.handOf(p0)
        assertFalse(c1 in nextHand && c2 in nextHand)
        assertEquals(customHand.size, nextHand.size) // maintains hand size

        // 3. Discard pile contains both cards
        assertTrue(c1 in next.discard)
        assertTrue(c2 in next.discard)

        // 4. Turn advanced
        assertEquals("p1", next.currentPlayer.id)
    }

    @Test
    fun testCraftPlace_failsWhenTacticalDisabled() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.SEVEN)
        val targetPos = BoardPosition(3, 4)

        var state = classicGame() // tactical disabled by default
        val p0 = state.currentPlayer
        val customHand = Hand(listOf(c1, c2))
        state = state.copy(hands = state.hands + (p0.id to customHand))

        val move = Move.CraftPlace(p0.id, c1, c2, targetPos)
        val result = GameEngine.applyMove(state, move)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Tactical crafting is disabled"))
    }

    @Test
    fun testCraftPlace_failsWhenCardsNotSameRank() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.EIGHT) // Different rank!
        val targetPos = BoardPosition(3, 4)

        var state = tacticalGame()
        val p0 = state.currentPlayer
        state = state.copy(hands = state.hands + (p0.id to Hand(listOf(c1, c2))))

        val move = Move.CraftPlace(p0.id, c1, c2, targetPos)
        val result = GameEngine.applyMove(state, move)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("same rank"))
    }

    @Test
    fun testCraftPlace_failsWhenPositionOccupiedOrCorner() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.SEVEN)
        var state = tacticalGame()
        val p0 = state.currentPlayer
        state = state.copy(hands = state.hands + (p0.id to Hand(listOf(c1, c2))))

        // Corner failure
        val cornerMove = Move.CraftPlace(p0.id, c1, c2, BoardPosition(0, 0))
        assertTrue(GameEngine.applyMove(state, cornerMove).isFailure)

        // Occupied failure
        val occupiedPos = BoardPosition(1, 1)
        state = state.copy(chips = state.chips.place(occupiedPos, Team.RED))
        val occupiedMove = Move.CraftPlace(p0.id, c1, c2, occupiedPos)
        assertTrue(GameEngine.applyMove(state, occupiedMove).isFailure)
    }

    // ==========================================
    // 3. CraftRemove Tests (Suited Connector -> Snipe)
    // ==========================================

    @Test
    fun testCraftRemove_successfulExecution() {
        val c1 = Card(Suit.HEARTS, Rank.EIGHT)
        val c2 = Card(Suit.HEARTS, Rank.NINE)
        val opponentPos = BoardPosition(5, 5)

        var state = tacticalGame()
        val p0 = state.currentPlayer
        val customHand = Hand(listOf(c1, c2, Card(Suit.CLUBS, Rank.TWO), Card(Suit.DIAMONDS, Rank.THREE)))
        // Place opponent chip on board
        state = state.copy(
            hands = state.hands + (p0.id to customHand),
            chips = state.chips.place(opponentPos, Team.RED)
        )

        val move = Move.CraftRemove(p0.id, c1, c2, opponentPos)
        val next = GameEngine.applyMove(state, move).getOrThrow()

        // 1. Opponent chip removed
        assertNull(next.chips.at(opponentPos))

        // 2. Discarded and redrawn
        assertEquals(customHand.size, next.handOf(p0).size)
        assertTrue(c1 in next.discard && c2 in next.discard)

        // 3. Turn advanced
        assertEquals("p1", next.currentPlayer.id)
    }

    @Test
    fun testCraftRemove_failsWhenChipLockedOrOwnTeam() {
        val c1 = Card(Suit.HEARTS, Rank.EIGHT)
        val c2 = Card(Suit.HEARTS, Rank.NINE)
        val targetPos = BoardPosition(5, 5)

        var state = tacticalGame()
        val p0 = state.currentPlayer
        state = state.copy(hands = state.hands + (p0.id to Hand(listOf(c1, c2))))

        // Fails when targeting own team
        val ownChipState = state.copy(chips = state.chips.place(targetPos, Team.BLUE))
        val ownMove = Move.CraftRemove(p0.id, c1, c2, targetPos)
        assertTrue(GameEngine.applyMove(ownChipState, ownMove).isFailure)

        // Fails when chip is locked in completed sequence
        val lockedSequence = ChipSequence(Team.RED, listOf(targetPos))
        val lockedState = state.copy(
            chips = state.chips.place(targetPos, Team.RED),
            completedSequence = listOf(lockedSequence)
        )
        val lockedMove = Move.CraftRemove(p0.id, c1, c2, targetPos)
        val result = GameEngine.applyMove(lockedState, lockedMove)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("locked"))
    }

    // ==========================================
    // 4. DivineWipe Tests (Straight Flush -> Clear Opponent)
    // ==========================================

    @Test
    fun testDivineWipe_wipesAllOpponentChipsAndLockedSequences() {
        val royalSpades = listOf(
            Card(Suit.SPADES, Rank.TEN),
            Card(Suit.SPADES, Rank.JACK),
            Card(Suit.SPADES, Rank.QUEEN),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.SPADES, Rank.ACE),
        )

        var state = tacticalGame()
        val p0 = state.currentPlayer

        // Setup opponent with 4 regular chips and 1 locked sequence of 5 chips
        val oppSeqPositions = listOf(
            BoardPosition(1, 1), BoardPosition(1, 2), BoardPosition(1, 3),
            BoardPosition(1, 4), BoardPosition(1, 5)
        )
        val oppOtherPositions = listOf(BoardPosition(4, 4), BoardPosition(5, 5))

        // Setup player's own chip and sequence
        val myPos = BoardPosition(8, 8)
        val mySeqPositions = listOf(
            BoardPosition(9, 1), BoardPosition(9, 2), BoardPosition(9, 3),
            BoardPosition(9, 4), BoardPosition(9, 5)
        )

        var chips = ChipMap()
        oppSeqPositions.forEach { chips = chips.place(it, Team.RED) }
        oppOtherPositions.forEach { chips = chips.place(it, Team.RED) }
        chips = chips.place(myPos, Team.BLUE)
        mySeqPositions.forEach { chips = chips.place(it, Team.BLUE) }

        val completedSequences = listOf(
            ChipSequence(Team.RED, oppSeqPositions),
            ChipSequence(Team.BLUE, mySeqPositions)
        )

        state = state.copy(
            hands = state.hands + (p0.id to Hand(royalSpades)),
            chips = chips,
            completedSequence = completedSequences
        )

        // Execute Divine Wipe
        val move = Move.DivineWipe(p0.id, royalSpades)
        val next = GameEngine.applyMove(state, move).getOrThrow()

        // 1. ALL opponent chips wiped out!
        oppSeqPositions.forEach { assertNull(next.chips.at(it), "Locked opponent chip at $it must be wiped") }
        oppOtherPositions.forEach { assertNull(next.chips.at(it), "Opponent chip at $it must be wiped") }

        // 2. Opponent completed sequences wiped!
        assertEquals(0, next.sequencesOf(Team.RED))

        // 3. Own team chips and sequences are 100% PRESERVED!
        assertEquals(Team.BLUE, next.chips.at(myPos))
        mySeqPositions.forEach { assertEquals(Team.BLUE, next.chips.at(it)) }
        assertEquals(1, next.sequencesOf(Team.BLUE))

        // 4. 5 cards discarded, 5 cards redrawn
        assertEquals(5, next.handOf(p0).size)
        royalSpades.forEach { assertTrue(it in next.discard) }

        // 5. Turn advanced
        assertEquals("p1", next.currentPlayer.id)
    }

    @Test
    fun testDivineWipe_failsIfNotStraightFlush() {
        val invalidCards = listOf(
            Card(Suit.SPADES, Rank.TEN),
            Card(Suit.HEARTS, Rank.JACK), // Different suit!
            Card(Suit.SPADES, Rank.QUEEN),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.SPADES, Rank.ACE),
        )

        var state = tacticalGame()
        val p0 = state.currentPlayer
        state = state.copy(hands = state.hands + (p0.id to Hand(invalidCards)))

        val move = Move.DivineWipe(p0.id, invalidCards)
        val result = GameEngine.applyMove(state, move)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("suited straight flush"))
    }

    // ==========================================
    // 5. LegalMoves Integration Tests
    // ==========================================

    @Test
    fun testLegalMoves_tacticalDisabled_onlyClassicMoves() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.SEVEN)
        var state = classicGame()
        val p0 = state.currentPlayer
        state = state.copy(hands = state.hands + (p0.id to Hand(listOf(c1, c2))))

        val moves = GameEngine.legalMoves(state, p0.id)
        assertTrue(moves.all { it is Move.Place || it is Move.Remove || it is Move.SwapDeadCard })
        assertTrue(moves.none { it is Move.CraftPlace || it is Move.CraftRemove || it is Move.DivineWipe })
    }

    @Test
    fun testLegalMoves_tacticalEnabled_includesCraftMoves() {
        val c1 = Card(Suit.SPADES, Rank.SEVEN)
        val c2 = Card(Suit.HEARTS, Rank.SEVEN)
        var state = tacticalGame()
        val p0 = state.currentPlayer
        state = state.copy(hands = state.hands + (p0.id to Hand(listOf(c1, c2))))

        val moves = GameEngine.legalMoves(state, p0.id)
        assertTrue(moves.any { it is Move.CraftPlace })
    }
}
