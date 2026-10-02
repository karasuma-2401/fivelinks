package com.karasuma.fivelinks.fivelinks_cmp.domain

object SequenceDetector {
    fun findSequence(state: GameState, team: Team): List<ChipSequence> {
        val existing = state.completedSequence.filter { it.team == team }
        // Track how many times each non-corner position has been used in completed sequences
        val usage = mutableMapOf<Int, Int>()
        for (sequence in existing) {
            for (pos in sequence.positions) {
                if (state.board.isCorner(pos)) continue
                usage[pos.flatIndex] = (usage[pos.flatIndex] ?: 0) + 1
            }
        }
        val existingSets: List<Set<BoardPosition>> = existing.map { it.positions.toSet() }
        val newlyFound = mutableListOf<ChipSequence>()

        for (line in BoardLines.all) {
            val status = lineStatus(state, line, team)
            // Check opponent = 0 & all five positions controlled by team
            if (!status.openForTeam || status.controlledByTeam != 5) continue
            val lineSet = line.toSet()
            if (existingSets.any { it == lineSet }) continue
            if (newlyFound.any { it.positions.toSet() == lineSet }) continue

            // A normal chip can belong to at most two sequences (e.g. at an intersection)
            val anyMaxedOut = line.any { !state.board.isCorner(it) && (usage[it.flatIndex] ?: 0) >= 2 }
            if (anyMaxedOut) continue

            // In Sequence rules, two sequences can share AT MOST ONE space.
            // 6 chips in a row is NOT 2 sequences (only 1). To make 2 sequences in a line, 9 chips are required.
            // Therefore, a new sequence can reuse at most 1 non-corner chip already part of an existing/found sequence.
            val reusedChips = line.count { p -> !state.board.isCorner(p) && (usage[p.flatIndex] ?: 0) >= 1 }
            if (reusedChips > 1) continue

            newlyFound += ChipSequence(team, line)
            for (pos in line) {
                if (state.board.isCorner(pos)) continue
                usage[pos.flatIndex] = (usage[pos.flatIndex] ?: 0) + 1
            }
        }
        return newlyFound
    }
}