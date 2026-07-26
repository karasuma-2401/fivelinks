package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Cards
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

class ActionCodec (
    private val cardIndex: Map<Card, Int> = Cards.fullDeck.withIndex().associate { it.value to it.index }
) {
    val numCards: Int get() = Cards.fullDeck.size
    val placeSize: Int get() = numCards * 100
    val removeSize: Int get() = numCards * 100
    val swapSize: Int get() = numCards
    val maxActions: Int get() = placeSize + removeSize + swapSize

    fun encode(move: Move): Int {
         val ci = cardIndex.getValue(move.card)
        return when (move) {
            is Move.Place -> ci * 100 + move.position.flatIndex
            is Move.Remove -> placeSize + ci * 100 + move.position.flatIndex
            is Move.SwapDeadCard -> placeSize + removeSize + ci
        }
    }
    fun legalMask (state: GameState, playerId: PlayerId): BooleanArray {
        val mask = BooleanArray(maxActions)
        for (move in GameEngine.legalMoves(state, playerId)) {
            mask[encode(move)] = true
        }
        return mask
    }
}