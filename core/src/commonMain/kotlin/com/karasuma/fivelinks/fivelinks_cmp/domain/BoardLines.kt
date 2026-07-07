package com.karasuma.fivelinks.fivelinks_cmp.domain

object BoardLines {
    private val DIRECTION = listOf(
        0 to 1,
        1 to 0,
        1 to 1, // principal diagonal
        1 to -1 // second diagonal
    )
    val all: List<List<BoardPosition>> = buildList {
        for (start in BoardPosition.all) {
            for ((r, c) in DIRECTION) {
                val endRow = start.row + r * 4
                val endCol = start.column + c * 4

                if (endRow in 0..9 && endCol in 0..9) {
                    add((0..4).map { BoardPosition(start.row + r * it, start.column + c * it) })
                }
            }
        }
    }
    // check 1 cell belong to what lines ?
    // group by position
    val throughPosition: Map<BoardPosition, List<List<BoardPosition>>> = all.flatMap { line -> line.map { it to line } }.groupBy ( {it.first}, {it.second} )
}
data class LineStatus (
    val own: Int,
    val opponent: Int,
    val empty: Int,
    val corner: Int
) {
    val openForTeam: Boolean get() = opponent == 0
    val controlledByTeam: Int get() = own + corner
}

fun lineStatus(state: GameState, line: List<BoardPosition>, team: Team): LineStatus {
    var own = 0
    var opponent = 0
    var empty = 0
    var corner = 0
    for (position in line) {
        when  {
            state.board.isCorner(position) -> corner++
            else -> {
                when (val chip = state.chips.at(position)) {
                    null -> empty++
                    team -> own++
                    else -> opponent++
                }
            }
        }
    }
    return LineStatus(own, opponent, empty, corner)
}

