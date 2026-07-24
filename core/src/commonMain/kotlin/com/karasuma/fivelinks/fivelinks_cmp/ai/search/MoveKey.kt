package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

sealed interface MoveKey {
    data class Place(val card: Card, val position: BoardPosition): MoveKey
    data class Remove(val card: Card, val position: BoardPosition): MoveKey
    data class Swap(val card: Card): MoveKey

    companion object {
        fun from(move: Move): MoveKey = when (move) {
            is Move.Place -> Place(move.card, move.position)
            is Move.Remove -> Remove(move.card, move.position)
            is Move.SwapDeadCard -> Swap(move.card)
        }
    }
}

fun MoveKey.toMove(playerId: PlayerId): Move = when (this) {
    is MoveKey.Place -> Move.Place(playerId, card, position)
    is MoveKey.Remove -> Move.Remove(playerId, card, position)
    is MoveKey.Swap -> Move.SwapDeadCard(playerId, card)
}