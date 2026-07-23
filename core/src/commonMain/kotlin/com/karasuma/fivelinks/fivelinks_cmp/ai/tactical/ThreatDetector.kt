package com.karasuma.fivelinks.fivelinks_cmp.ai.tactical

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardLines
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.lineStatus

object ThreatDetector {
    fun countOpenFours(state: GameState, team: Team): Int {
        var count = 0
        for (line in BoardLines.all) {
            val s = lineStatus(state, line, team)
            if (s.openForTeam && s.controlledByTeam == 4 && s.empty == 1)  count++
        }
        return count
    }
    fun openFoursEmptyCells(state: GameState, team: Team): Set<BoardPosition> {
        val cells = mutableSetOf<BoardPosition>()
        for (line in BoardLines.all) {
            val s = lineStatus(state, line, team)
            if (s.openForTeam && s.controlledByTeam == 4 && s.empty == 1) {
                line.firstOrNull { state.chips.at(it) == null && !state.board.isCorner(it) }
                ?.let { cells.add(it) }
            }
        }
        return cells
    }
}