package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Deck(val cards: List<Card>) {
    val size: Int get() = cards.size
    operator fun get(position: Int) = cards[position]
    operator fun contains(card: Card): Boolean = cards.contains(card)

    fun isEmpty(): Boolean = cards.isEmpty()
    fun draw(): Pair<Card?, Deck> {
        return if (cards.isEmpty()) {
            null to this
        }
        else cards.first() to Deck(cards.drop(1))
    }

    companion object {
        fun twoShuffleDeck(seed: Long): Deck {
            val pool = Cards.fullDeck + Cards.fullDeck
            pool.shuffled(Random(seed))
            return Deck(pool)
        }
    }
}