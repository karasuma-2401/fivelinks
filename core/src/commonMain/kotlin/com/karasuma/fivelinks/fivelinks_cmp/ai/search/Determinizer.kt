package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Deck
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlin.random.Random

class Determinizer(private val random: Random = Random.Default) {

    fun sample(state: GameState, viewerId: PlayerId): GameState {
        val viewer = state.players.first { it.id == viewerId }
        val unknown = mutableListOf<Card>()
        unknown += state.deck.cards
        for (p in state.players) {
            if (p.id != viewerId) unknown += state.handOf(p).cards
        }
        unknown.shuffle(random)

        val newHands = state.hands.toMutableMap()
        var idx = 0
        for (p in state.players) {
            if (p.id == viewerId) continue
            val n = state.handOf(p).size
            val dealt = unknown.subList(idx, idx + n).toList()
            idx += n
            newHands[p.id] = Hand(dealt)
        }
        val rest = unknown.subList(idx, unknown.size).toList()
        return state.copy(
            hands = newHands,
            deck = Deck(rest),
        )
    }
}
