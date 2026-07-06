package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
data class Hand(val cards: List<Card>) {
    // return number of cards on hand
    val size: Int get() = cards.size
    operator fun get(position: Int) = cards[position]
    operator fun contains (card: Card): Boolean = cards.contains(card)


    fun without(card: Card): Hand {
        val index = cards.indexOf(card)
        require(index >= 0) { "Card $card is not in hand" }
        return Hand(cards.toMutableList().apply { removeAt(index) })
    }

    fun with(card: Card): Hand {
        return Hand(cards.toMutableList().apply { add(card) })
    }

}