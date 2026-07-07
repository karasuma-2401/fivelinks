package com.karasuma.fivelinks.fivelinks_cmp.domain

object SequenceDetector {
    fun findSequence(state: GameState, team: Team): List<ChipSequence> {
        val existing = state.completedSequence.filter { it.team == team }
        // define each chip have ? completed sequences
        val usage = mutableMapOf<Int, Int>()
        for (sequence in existing) {
            for (pos in sequence.positions) {
                if (state.board.isCorner(pos)) continue
                usage[pos.flatIndex] = (usage[pos.flatIndex] ?: 0) + 1
            }
        }
        // ignore same five positions
        val existingSet: List<Set<BoardPosition>> = existing.map { it.positions.toSet() }
        val newlyFound = mutableListOf<ChipSequence>()

        for (line in BoardLines.all) {
            val status = lineStatus(state,line, team)
            // check opponent = 0 & five positions is controlled by teams
            if (!status.openForTeam || status.controlledByTeam != 5) continue
            val lineSet = line.toSet()
            if (existingSet.any { it.containsAll(lineSet) } || usage.values.any { it == 5}) continue
            if (newlyFound.any { it.positions.toSet() == lineSet }) continue

            // a normal chip only belong to at most two sequences
            val anyMaxedOut = line.any { !state.board.isCorner(it)  && (usage[it.flatIndex] ?: 0) >= 2}
            if (anyMaxedOut) continue

            // count chips in line are already used by previous sequences
            val reusedChips = line.count { p -> !state.board.isCorner(p) && (usage[p.flatIndex] ?: 0) >= 2 }
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