package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
data class BoardPosition(val row: Int, val column: Int) {
    init {
        require(row in 0..9) { "Row must be between 0 and 9" }
        require(column in 0..9) { "Column must be between 0 and 9" }
    }

    val flatIndex: Int get() = row * 10 + column
    override fun toString(): String {
        return "$row-$column"
    }
    companion object {
        val all: List<BoardPosition> = buildList {
            for (row in 0..9) {
                for (column in 0..9) {
                    add(BoardPosition(row, column))
                }
            }
        }
        // corners have star point
        val corners: List<BoardPosition> = buildList {
            add(BoardPosition(0, 0))
            add(BoardPosition(0, 9))
            add(BoardPosition(9, 0))
            add(BoardPosition(9, 9))
        }
        fun fromFlatIndex(index: Int): BoardPosition {
            val row = index / 10
            val column = index % 10
            return BoardPosition(row, column)
        }
    }
}

@Serializable
sealed interface Cell {
    @Serializable
    data object Corner : Cell

    @Serializable
    data class Slot(val card: Card) : Cell
}

@Serializable
data class Board (val cells: List<Cell>) {
    init {
        require(cells.size == 100) { "Board must have 100 cells" }
        require(BoardPosition.corners.all { cells[it.flatIndex] is Cell.Corner }) { "Board must have all Corners"}
    }
    operator fun get(position: BoardPosition): Cell = cells[position.flatIndex]

    // return card in position
    fun cardAt(position: BoardPosition): Card = (cells[position.flatIndex] as Cell.Slot).card

    fun isCorner(position: BoardPosition): Boolean = cells[position.flatIndex] is Cell.Corner

    // return all position belong to a card
    fun positionsOf(card: Card): List<BoardPosition> = BoardPosition.all.filter { cardAt(it) == card }

    companion object {
        val standard: Board = BoardLayout.standardLayout
    }
}