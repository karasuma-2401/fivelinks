package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.isOneEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.domain.isTwoEyedJack

/**
 * NCHW encoder. Channel layout must match [encoder_spec.json].
 *
 * 0 me_chips, 1 opp_chips, 2 empty, 3 corners, 4 locked,
 * 5 my_hand_playable,
 * 6–8 team1/2/3 seq count (RED/BLUE/GREEN, normalized by sequenceToWin),
 * 9–10 team1/2 remaining-to-win (RED/BLUE, normalized),
 * 11 deck_size_norm
 */
class StateEncoder(
    val version: Int = 1,
    val channels: Int = CHANNELS,
    val height: Int = 10,
    val width: Int = 10,
) {
    val size: Int get() = channels * height * width

    fun encode(state: GameState, perspective: Team): FloatArray {
        require(channels == CHANNELS) { "StateEncoder.channels must be $CHANNELS (see encoder_spec.json)" }
        val out = FloatArray(size)
        fun idx(c: Int, row: Int, col: Int) = c * 100 + row * 10 + col
        fun fillChannel(c: Int, value: Float) {
            val base = c * 100
            for (i in 0 until 100) out[base + i] = value
        }

        for (row in 0..9) for (col in 0..9) {
            val pos = BoardPosition(row, col)
            val team = state.chips.at(pos)
            when {
                team == perspective -> out[idx(0, row, col)] = 1f
                team != null -> out[idx(1, row, col)] = 1f
                !state.board.isCorner(pos) -> out[idx(2, row, col)] = 1f
            }
            if (state.board.isCorner(pos)) out[idx(3, row, col)] = 1f
        }

        for (seq in state.completedSequence) {
            for (pos in seq.positions) {
                out[idx(4, pos.row, pos.column)] = 1f
            }
        }

        markHandPlayable(state, perspective, out)

        val toWin = state.config.sequenceToWin.coerceAtLeast(1).toFloat()
        TEAM_ORDER.forEachIndexed { i, team ->
            fillChannel(6 + i, state.sequencesOf(team).toFloat() / toWin)
        }
        // Remaining-to-win for RED / BLUE (channels 9–10)
        fillChannel(9, (toWin - state.sequencesOf(Team.RED)).coerceAtLeast(0f) / toWin)
        fillChannel(10, (toWin - state.sequencesOf(Team.BLUE)).coerceAtLeast(0f) / toWin)

        fillChannel(11, state.deck.size.toFloat() / MAX_DECK_SIZE)

        return out
    }

    private fun markHandPlayable(state: GameState, perspective: Team, out: FloatArray) {
        fun idx(c: Int, row: Int, col: Int) = c * 100 + row * 10 + col
        for (player in state.players) {
            if (player.team != perspective) continue
            for (card in state.handOf(player).cards.distinct()) {
                when {
                    card.isTwoEyedJack() -> {
                        for (pos in BoardPosition.all) {
                            if (state.board.isCorner(pos)) continue
                            if (state.chips.contains(pos)) continue
                            out[idx(5, pos.row, pos.column)] = 1f
                        }
                    }
                    card.isOneEyedJack() -> {
                        for (pos in BoardPosition.all) {
                            val chip = state.chips.at(pos) ?: continue
                            if (chip == player.team) continue
                            if (state.completedSequence.any { pos in it.positions }) continue
                            out[idx(5, pos.row, pos.column)] = 1f
                        }
                    }
                    else -> {
                        for (pos in state.board.positionsOf(card)) {
                            if (!state.chips.contains(pos)) {
                                out[idx(5, pos.row, pos.column)] = 1f
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val CHANNELS = 12
        const val MAX_DECK_SIZE = 104f // two full 52-card decks
        val TEAM_ORDER = listOf(Team.RED, Team.BLUE, Team.GREEN)
    }
}
