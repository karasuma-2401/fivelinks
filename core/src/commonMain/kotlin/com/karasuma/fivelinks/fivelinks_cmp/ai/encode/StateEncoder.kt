package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team

class StateEncoder(
    val version: Int = 1,
    val channels: Int = 10,
    val height: Int = 10,
    val width: Int = 10
) {
    val size: Int get() = channels * height * width
    // NCHW flat float array
    fun encode(state: GameState, perspective: Team): FloatArray {
        val out = FloatArray(size)
        fun idx(c: Int, row: Int, col: Int) = c * 100 + row * 10 + col

        for (row in 0..9) for (col in 0..9) {
            val pos = BoardPosition(row, col)
            val team = state.chips.at(pos)
            when {
                team == perspective -> out[idx(0, row, col)] = 1f
                team != null -> out[idx(1, row, col)] = 1f
                !state.board.isCorner(pos) -> out[idx(2, row, col)] = 1f
            }
            if (state.board.isCorner(pos)) out[idx(3,row, col )] = 1f
        }
        for (seq in state.completedSequence) {
            for (pos in seq.positions)
                out[idx(4, pos.row, pos.column)] = 1f
        }
        return out
    }
}