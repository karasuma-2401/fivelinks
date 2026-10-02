package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SequenceDetectorTest {

    private fun testState(
        chips: Map<BoardPosition, Team> = emptyMap(),
        completedSequences: List<ChipSequence> = emptyList()
    ): GameState {
        val config = GameConfig.soloVsAi(seed = 42L)
        val players = listOf(
            Player(id = "p0", name = "Human", team = Team.RED, isAi = false),
            Player(id = "p1", name = "AI", team = Team.BLUE, isAi = true),
        )
        val baseState = GameEngine.initialize(config, players)
        var chipMap = ChipMap()
        for ((pos, team) in chips) {
            chipMap = chipMap.place(pos, team)
        }
        return baseState.copy(
            chips = chipMap,
            completedSequence = completedSequences
        )
    }

    @Test
    fun fiveInARow_formsExactlyOneSequence() {
        val chips = (1..5).associate { col -> BoardPosition(2, col) to Team.BLUE }
        val state = testState(chips = chips)

        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        assertEquals(1, sequences.size)
        assertEquals(Team.BLUE, sequences.first().team)
        assertEquals(5, sequences.first().positions.size)
    }

    @Test
    fun sixInARow_formsOnlyOneSequence_notTwo() {
        // 6 chips in a row: cols 1 to 6 in row 2
        val chips = (1..6).associate { col -> BoardPosition(2, col) to Team.BLUE }
        val state = testState(chips = chips)

        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        // CRITICAL: 6 chips in a row must ONLY count as 1 sequence, never 2!
        assertEquals(1, sequences.size)
    }

    @Test
    fun sevenInARow_formsOnlyOneSequence() {
        val chips = (1..7).associate { col -> BoardPosition(2, col) to Team.BLUE }
        val state = testState(chips = chips)

        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        assertEquals(1, sequences.size)
    }

    @Test
    fun eightInARow_formsOnlyOneSequence() {
        val chips = (1..8).associate { col -> BoardPosition(2, col) to Team.BLUE }
        val state = testState(chips = chips)

        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        assertEquals(1, sequences.size)
    }

    @Test
    fun nineInARow_formsTwoSequences_sharingOneChip() {
        // In official Sequence rules, 9 chips in a row can form 2 sequences (sharing chip 5)
        val chips = (1..9).associate { col -> BoardPosition(2, col) to Team.BLUE }
        val state = testState(chips = chips)

        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        assertEquals(2, sequences.size)
    }

    @Test
    fun existingSequence_addingSixthChip_doesNotCreateSecondSequence() {
        // Sequence 1 already completed with cols 1..5
        val seq1Positions = (1..5).map { col -> BoardPosition(3, col) }
        val existingSeq = ChipSequence(Team.BLUE, seq1Positions)

        // Board now has 6 chips (cols 1..6)
        val chips = (1..6).associate { col -> BoardPosition(3, col) to Team.BLUE }
        val state = testState(
            chips = chips,
            completedSequences = listOf(existingSeq)
        )

        val newSequences = SequenceDetector.findSequence(state, Team.BLUE)
        // Adding the 6th chip must not award a new sequence
        assertEquals(0, newSequences.size)
    }

    @Test
    fun crossingSequences_sharingOneChip_formsTwoSequences() {
        // Horizontal: row 3, cols 1..5
        // Vertical: rows 1..5, col 3
        // Intersection at (3, 3)
        val chips = mutableMapOf<BoardPosition, Team>()
        for (col in 1..5) chips[BoardPosition(3, col)] = Team.BLUE
        for (row in 1..5) chips[BoardPosition(row, 3)] = Team.BLUE

        val state = testState(chips = chips)
        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        assertEquals(2, sequences.size)
    }

    @Test
    fun cornerSharing_twoSequencesUsingSameCorner_bothValid() {
        // Corner at (0, 0)
        // Horizontal: Corner (0,0) + cols 1..4 in row 0
        // Vertical: Corner (0,0) + rows 1..4 in col 0
        val chips = mutableMapOf<BoardPosition, Team>()
        for (col in 1..4) chips[BoardPosition(0, col)] = Team.BLUE
        for (row in 1..4) chips[BoardPosition(row, 0)] = Team.BLUE

        val state = testState(chips = chips)
        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        assertEquals(2, sequences.size)
    }

    @Test
    fun cornerPlusFiveChips_formsOnlyOneSequence() {
        // Corner (0, 0) + chips at (0, 1)..(0, 5) (5 chips next to corner)
        val chips = (1..5).associate { col -> BoardPosition(0, col) to Team.BLUE }
        val state = testState(chips = chips)

        val sequences = SequenceDetector.findSequence(state, Team.BLUE)
        // Should only form 1 sequence, not 2
        assertEquals(1, sequences.size)
    }

    @Test
    fun gameEngine_placingSixthChip_doesNotTriggerWin() {
        // Start game
        val config = GameConfig.soloVsAi(seed = 100L)
        val players = listOf(
            Player(id = "p0", name = "Human", team = Team.RED, isAi = false),
            Player(id = "p1", name = "AI", team = Team.BLUE, isAi = true),
        )
        val baseState = GameEngine.initialize(config, players)

        // Setup 5 chips in row 4, cols 1..5 already completed as sequence
        val seq1 = (1..5).map { BoardPosition(4, it) }
        var chipMap = ChipMap()
        for (pos in seq1) chipMap = chipMap.place(pos, Team.BLUE)

        // Give AI a card that matches BoardPosition(4, 6)
        val targetPos = BoardPosition(4, 6)
        val cardAtTarget = baseState.board.cardAt(targetPos)!!
        val aiPlayer = baseState.players[1]
        val aiHand = Hand(listOf(cardAtTarget))

        val stateWithExistingSeq = baseState.copy(
            chips = chipMap,
            completedSequence = listOf(ChipSequence(Team.BLUE, seq1)),
            hands = baseState.hands + (aiPlayer.id to aiHand),
            currentPlayerIndex = 1
        )

        // AI places 6th chip at (4, 6)
        val move = Move.Place(aiPlayer.id, cardAtTarget, targetPos)
        val nextState = GameEngine.applyMove(stateWithExistingSeq, move).getOrThrow()

        // Verify that total completedSequence is still 1, and AI does NOT win!
        assertEquals(1, nextState.completedSequence.size)
        assertNull(nextState.winner)
    }
}
