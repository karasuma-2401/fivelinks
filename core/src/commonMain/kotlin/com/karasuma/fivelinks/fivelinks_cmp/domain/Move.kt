package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface Move {
    val playerId: PlayerId
    val card: Card

    @Serializable
    data class Place(override val playerId: PlayerId, override val card: Card, val position: BoardPosition): Move

    @Serializable
    data class Remove(override val playerId: PlayerId, override val card: Card, val position: BoardPosition): Move

    @Serializable
    data class SwapDeadCard(override val playerId: PlayerId, override val card: Card): Move

    @Serializable
    data class CraftPlace(
        override val playerId: PlayerId,
        val card1: Card,
        val card2: Card,
        val position: BoardPosition
    ): Move {
        override val card: Card get() = card1
    }

    @Serializable
    data class CraftRemove(
        override val playerId: PlayerId,
        val card1: Card,
        val card2: Card,
        val position: BoardPosition
    ): Move {
        override val card: Card get() = card1
    }

    @Serializable
    data class DivineWipe(
        override val playerId: PlayerId,
        val cards: List<Card>
    ): Move {
        override val card: Card get() = cards.first()
    }
}