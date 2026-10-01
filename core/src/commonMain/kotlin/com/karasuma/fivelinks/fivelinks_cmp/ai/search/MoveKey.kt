package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

sealed interface MoveKey {
    data class Place(val card: Card, val position: BoardPosition): MoveKey
    data class Remove(val card: Card, val position: BoardPosition): MoveKey
    data class Swap(val card: Card): MoveKey
    data class CraftPlace(val card1: Card, val card2: Card, val position: BoardPosition): MoveKey
    data class CraftRemove(val card1: Card, val card2: Card, val position: BoardPosition): MoveKey
    data class DivineWipe(val cards: List<Card>): MoveKey

    companion object {
        fun from(move: Move): MoveKey = when (move) {
            is Move.Place -> Place(move.card, move.position)
            is Move.Remove -> Remove(move.card, move.position)
            is Move.SwapDeadCard -> Swap(move.card)
            is Move.CraftPlace -> CraftPlace(move.card1, move.card2, move.position)
            is Move.CraftRemove -> CraftRemove(move.card1, move.card2, move.position)
            is Move.DivineWipe -> DivineWipe(move.cards)
        }
    }
}

fun MoveKey.toMove(playerId: PlayerId): Move = when (this) {
    is MoveKey.Place -> Move.Place(playerId, card, position)
    is MoveKey.Remove -> Move.Remove(playerId, card, position)
    is MoveKey.Swap -> Move.SwapDeadCard(playerId, card)
    is MoveKey.CraftPlace -> Move.CraftPlace(playerId, card1, card2, position)
    is MoveKey.CraftRemove -> Move.CraftRemove(playerId, card1, card2, position)
    is MoveKey.DivineWipe -> Move.DivineWipe(playerId, cards)
}