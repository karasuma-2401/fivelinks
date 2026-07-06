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
}