package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlin.math.abs

object TacticalPatterns {

    fun isPair(c1: Card, c2: Card): Boolean {
        return c1.rank == c2.rank
    }

    fun isSuitedConnector(c1: Card, c2: Card): Boolean {
        if (c1.suit != c2.suit) return false
        val diff = abs(c1.rank.value - c2.rank.value)
        return diff == 1 ||
                (c1.rank == Rank.ACE && c2.rank == Rank.TWO) ||
                (c1.rank == Rank.TWO && c2.rank == Rank.ACE)
    }

    fun isStraightFlush(cards: List<Card>): Boolean {
        if (cards.size != 5) return false
        val firstSuit = cards[0].suit
        if (!cards.all { it.suit == firstSuit }) return false
        val values = cards.map { it.rank.value }.sorted()
        if (values.distinct().size != 5) return false

        // Wheel straight: A-2-3-4-5 (values: 2, 3, 4, 5, 14)
        if (values == listOf(2, 3, 4, 5, 14)) return true

        // Standard straight: 5 consecutive values
        return (0..4).all { i -> values[i] == values[0] + i }
    }

    fun findPairs(hand: Hand): List<Pair<Card, Card>> {
        val pairs = mutableListOf<Pair<Card, Card>>()
        val byRank = hand.cards.groupBy { it.rank }
        for ((_, cards) in byRank) {
            if (cards.size >= 2) {
                pairs.add(cards[0] to cards[1])
            }
        }
        return pairs
    }

    fun findSuitedConnectors(hand: Hand): List<Pair<Card, Card>> {
        val connectors = mutableListOf<Pair<Card, Card>>()
        val bySuit = hand.cards.groupBy { it.suit }
        for ((_, cards) in bySuit) {
            if (cards.size < 2) continue
            for (i in 0 until cards.size - 1) {
                for (j in i + 1 until cards.size) {
                    if (isSuitedConnector(cards[i], cards[j])) {
                        connectors.add(cards[i] to cards[j])
                    }
                }
            }
        }
        return connectors
    }

    fun findStraightFlush(hand: Hand): List<Card>? {
        val cardsBySuit = hand.cards.groupBy { it.suit }
        for ((_, suitedCards) in cardsBySuit) {
            if (suitedCards.size < 5) continue
            val combinations = combinations5(suitedCards)
            for (comb in combinations) {
                if (isStraightFlush(comb)) return comb
            }
        }
        return null
    }

    private fun combinations5(list: List<Card>): List<List<Card>> {
        val result = mutableListOf<List<Card>>()
        val n = list.size
        for (i in 0 until n - 4) {
            for (j in i + 1 until n - 3) {
                for (k in j + 1 until n - 2) {
                    for (l in k + 1 until n - 1) {
                        for (m in l + 1 until n) {
                            result.add(listOf(list[i], list[j], list[k], list[l], list[m]))
                        }
                    }
                }
            }
        }
        return result
    }
}
