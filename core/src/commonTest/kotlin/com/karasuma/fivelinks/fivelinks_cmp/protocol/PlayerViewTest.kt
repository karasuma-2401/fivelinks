package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class PlayerViewTest {

    private fun onlineGame(): GameState = GameEngine.initialize(
        GameConfig.tactical(playerCount = 2, teams = 2, seed = 987_654_321L),
        listOf(
            Player(id = "host", name = "Host", team = Team.BLUE),
            Player(id = "guest", name = "Guest", team = Team.RED),
        )
    )

    @Test
    fun viewer_seesOnlyTheirOwnHand() {
        val state = onlineGame()

        val hostView = state.viewFor("host")
        val guestView = state.viewFor("guest")

        assertEquals(state.hands.getValue("host"), hostView.hand)
        assertEquals(state.hands.getValue("guest"), guestView.hand)
        assertEquals(mapOf("host" to 7, "guest" to 7), hostView.handSizes)
        assertEquals(state.deck.size, hostView.deckSize)
    }

    @Test
    fun hiddenInformation_neverReachesTheWire() {
        val state = onlineGame()
        assertNotEquals(0L, state.config.seed)

        val view = state.viewFor("host")
        val json = ProtocolJson.encodeToString(PlayerView.serializer(), view)

        // The seed would let a client rebuild the deck, so it is zeroed.
        assertEquals(0L, view.config.seed)
        assertFalse("\"deck\"" in json, "deck order leaked")
        assertFalse("\"hands\"" in json, "opponent hand leaked")
        assertFalse("987654321" in json, "seed leaked")
    }

    @Test
    fun view_followsTheGame() {
        var state = onlineGame()
        val move = GameEngine.legalMoves(state, "host").first()
        state = GameEngine.applyMove(state, move).getOrThrow()

        val view = state.viewFor("guest")

        assertEquals(move, view.lastMove)
        assertEquals("guest", view.currentPlayer.id)
        assertEquals(state.turnNumber, view.turnNumber)
        assertEquals(state.chips, view.chips)
        assertFalse(view.isGameOver)
    }

    @Test
    fun strangers_getNoView() {
        assertFailsWith<IllegalArgumentException> { onlineGame().viewFor("nobody") }
    }
}
