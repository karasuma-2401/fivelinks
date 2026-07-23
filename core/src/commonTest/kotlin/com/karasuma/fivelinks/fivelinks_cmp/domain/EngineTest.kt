package com.karasuma.fivelinks.fivelinks_cmp.domain

import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTest {

    private fun twoPlayers(seed: Long = 42L): GameState {
        val config = GameConfig.soloVsAi(seed)
        val players = listOf(
            Player(id = "p0", name = "Human", team = Team.RED, isAi = false),
            Player(id = "p1", name = "AI", team = Team.BLUE, isAi = true),
        )
        return GameEngine.initialize(config, players)
    }

    @Test
    fun twoShuffleDeck_isDeterministicPerSeed_andActuallyShuffles() {
        val a = Deck.twoShuffleDeck(123L)
        val b = Deck.twoShuffleDeck(123L)
        val c = Deck.twoShuffleDeck(456L)
        val unshuffled = Cards.fullDeck + Cards.fullDeck

        assertEquals(a.cards, b.cards)
        assertNotEquals(a.cards, c.cards)
        assertNotEquals(unshuffled, a.cards)
        assertEquals(104, a.size)
    }

    @Test
    fun applyPlace_usesMovePlayerId_notPlayersFirst() {
        var state = twoPlayers(seed = 7L)
        val p0Move = GameEngine.legalMoves(state, "p0").first { it is Move.Place }
        state = GameEngine.applyMove(state, p0Move).getOrThrow()
        assertEquals("p1", state.currentPlayer.id)

        val p1Place = GameEngine.legalMoves(state, "p1")
            .filterIsInstance<Move.Place>()
            .first()
        val after = GameEngine.applyMove(state, p1Place).getOrThrow()

        assertEquals(Team.BLUE, after.chips.at(p1Place.position))
        assertTrue(p1Place.card !in after.handOf(after.players.first { it.id == "p1" }))
        assertEquals(state.handOf(state.players[0]).size, after.handOf(after.players[0]).size)
    }

    @Test
    fun legalMoves_allPassValidator_andRemoveNeverTargetsEmpty() {
        var state = twoPlayers(seed = 99L)
        repeat(6) {
            if (state.isGameOver) return@repeat
            val pid = state.currentPlayer.id
            val moves = GameEngine.legalMoves(state, pid)
            assertTrue(moves.isNotEmpty(), "expected legal moves for $pid")
            for (move in moves) {
                assertTrue(
                    MoveValidator.validate(move, state).isSuccess,
                    "illegal move generated: $move",
                )
                if (move is Move.Remove) {
                    assertTrue(state.chips.contains(move.position), "Remove on empty ${move.position}")
                }
            }
            state = GameEngine.applyMove(state, moves.first()).getOrThrow()
        }
    }

    @Test
    fun gameState_roundTripsThroughProtocolJson() {
        val state = twoPlayers(seed = 11L)
        val json = ProtocolJson.encodeToString(GameState.serializer(), state)
        val decoded = ProtocolJson.decodeFromString(GameState.serializer(), json)
        assertEquals(state, decoded)

        val moved = GameEngine.applyMove(
            state,
            GameEngine.legalMoves(state, "p0").first(),
        ).getOrThrow()
        val json2 = ProtocolJson.encodeToString(GameState.serializer(), moved)
        assertEquals(moved, ProtocolJson.decodeFromString(GameState.serializer(), json2))
        assertNull(decoded.winner)
    }
}
