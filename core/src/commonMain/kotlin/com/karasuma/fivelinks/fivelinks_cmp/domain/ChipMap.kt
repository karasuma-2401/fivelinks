package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChipMap(val chips: Map<Int, Team> = emptyMap()) {
    fun at(position: BoardPosition): Team? = chips[position.flatIndex]

    fun place(position: BoardPosition, team: Team): ChipMap = ChipMap(chips + (position.flatIndex to team))

    fun remove(position: BoardPosition): ChipMap = ChipMap(chips - (position.flatIndex))

    operator fun contains(position: BoardPosition): Boolean = position.flatIndex in chips.keys

    fun positionFor(team: Team): List<BoardPosition> = chips.filter { it.value == team }.asSequence().map { BoardPosition.fromFlatIndex(it.key) }.toList()

    fun isEmpty(): Boolean = chips.isEmpty()

    val size: Int get() = chips.size
}