package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Cards
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionCodecTest {

    private val codec = ActionCodec()
    private val cardIndex = Cards.fullDeck.withIndex().associate { it.value to it.index }

    @Test
    fun encode_placeMove_isCorrect() {
        val card = Card(suit = Suit.SPADES, rank = Rank.ACE)
        val pos = BoardPosition(3, 4)
        val move = Move.Place("p0", card, pos)
        
        val expected = cardIndex.getValue(card) * 100 + pos.flatIndex
        assertEquals(expected, codec.encode(move))
    }

    @Test
    fun encode_removeMove_isCorrect() {
        val card = Card(suit = Suit.DIAMONDS, rank = Rank.JACK)
        val pos = BoardPosition(5, 5)
        val move = Move.Remove("p0", card, pos)

        val expected = codec.placeSize + cardIndex.getValue(card) * 100 + pos.flatIndex
        assertEquals(expected, codec.encode(move))
    }

    @Test
    fun encode_swapMove_isCorrect() {
        val card = Card(suit = Suit.CLUBS, rank = Rank.KING)
        val move = Move.SwapDeadCard("p0", card)

        val expected = codec.placeSize + codec.removeSize + cardIndex.getValue(card)
        assertEquals(expected, codec.encode(move))
    }

    @Test
    fun legalMask_matchesLegalMoves() {
        val config = GameConfig.forPlayer(2, 2, seed = 12L)
        val players = listOf(
            Player("p0", "Player 1", Team.RED, isAi = false),
            Player("p1", "Player 2", Team.BLUE, isAi = true)
        )
        val state = GameEngine.initialize(config, players)
        val playerId = state.currentPlayer.id

        val legalMoves = GameEngine.legalMoves(state, playerId)
        val mask = codec.legalMask(state, playerId)

        // Check that the number of true values in the mask equals the number of legal moves
        assertEquals(legalMoves.size, mask.count { it })

        // Check that every legal move corresponds to a true value in the mask
        for (move in legalMoves) {
            val encoded = codec.encode(move)
            assertTrue(mask[encoded], "Mask for move $move should be true")
        }
    }
}