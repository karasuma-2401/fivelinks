package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
enum class Suit(val symbol: String, val code: String) {
    CLUBS("♣", "C"),
    DIAMONDS("♦", "D"),
    HEARTS("♥", "H"),
    SPADES("♠", "S")
}
@Serializable
enum class Rank(val short: String, val value: Int) {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14)
}
@Serializable
data class Card(val suit: Suit, val rank: Rank) {
    override fun toString(): String {
        return "${rank.short}${suit.symbol}"
    }
    val code: String get() = "${rank.short}${suit.code}"
}

object Cards {
    val nonJackCards = buildList {
        Suit.entries.forEach { suit ->
            Rank.entries.forEach { rank ->
                if (rank != Rank.JACK)
                    add(Card(suit, rank))
            }
        }
    }
    val fullDeck = buildList {
        Suit.entries.forEach { suit ->
            Rank.entries.forEach { rank ->
                add(Card(suit, rank))
            }
        }
    }
}

fun Card.isJack(): Boolean = rank == Rank.JACK
fun Card.isTwoEyedJack(): Boolean = isJack() && (suit == Suit.CLUBS || suit == Suit.DIAMONDS)
fun Card.isOneEyedJack(): Boolean = isJack() && (suit == Suit.SPADES || suit == Suit.HEARTS)