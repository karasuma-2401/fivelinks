package com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.TacticalPatterns
import com.karasuma.fivelinks.fivelinks_cmp.domain.isOneEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.domain.isTwoEyedJack

/** The cards a player has picked from their hand, and what they allow on the board. */
data class CardSelection(
    val indices: Set<Int> = emptySet(),
    val tacticalAction: TacticalAction? = null,
    val placements: Set<BoardPosition> = emptySet(),
    val snipes: Set<BoardPosition> = emptySet(),
)

/** Works out what [player] can do with the cards at [indices] of their hand. */
fun cardSelection(state: GameState, player: Player, indices: Set<Int>): CardSelection {
    val hand = state.handOf(player)
    val selectedCards = indices.map { hand[it] }

    var activeTactical: TacticalAction? = null
    val validPlacements = mutableSetOf<BoardPosition>()
    val validSnipes = mutableSetOf<BoardPosition>()

    if (state.config.enableTacticalCrafting) {
        when {
            // 5 cards Straight Flush check
            selectedCards.size == 5 && TacticalPatterns.isStraightFlush(selectedCards) -> {
                activeTactical = TacticalAction.DivineWipe(selectedCards)
                // Highlight all opponent chips to signal wipe target
                for (pos in BoardPosition.all) {
                    val chip = state.chips.at(pos)
                    if (chip != null && chip != player.team) {
                        validSnipes.add(pos)
                    }
                }
            }
            // 2 cards Tactical Crafting check
            selectedCards.size == 2 -> {
                val c1 = selectedCards[0]
                val c2 = selectedCards[1]
                if (TacticalPatterns.isPair(c1, c2)) {
                    activeTactical = TacticalAction.PairWild(c1, c2)
                    // Wild Place: all open non-corner cells
                    for (pos in BoardPosition.all) {
                        if (!state.board.isCorner(pos) && !state.chips.contains(pos)) {
                            validPlacements.add(pos)
                        }
                    }
                } else if (TacticalPatterns.isSuitedConnector(c1, c2)) {
                    activeTactical = TacticalAction.ConnectorSnipe(c1, c2)
                    // Wild Snipe: all opponent chips not locked in sequence
                    for (pos in BoardPosition.all) {
                        val chip = state.chips.at(pos)
                        val isLocked = state.completedSequence.any { pos in it.positions }
                        if (chip != null && chip != player.team && !isLocked) {
                            validSnipes.add(pos)
                        }
                    }
                }
            }
        }
    }

    // Single Card Classic Play
    if (selectedCards.size == 1 && activeTactical == null) {
        val card = selectedCards.first()
        when {
            card.isTwoEyedJack() -> {
                for (pos in BoardPosition.all) {
                    if (!state.board.isCorner(pos) && !state.chips.contains(pos)) {
                        validPlacements.add(pos)
                    }
                }
            }
            card.isOneEyedJack() -> {
                for (pos in BoardPosition.all) {
                    val chip = state.chips.at(pos)
                    val isLocked = state.completedSequence.any { pos in it.positions }
                    if (chip != null && chip != player.team && !isLocked) {
                        validSnipes.add(pos)
                    }
                }
            }
            else -> {
                val openPositions = state.board.positionsOf(card).filter { !state.chips.contains(it) }
                validPlacements.addAll(openPositions)
            }
        }
    }

    return CardSelection(indices, activeTactical, validPlacements, validSnipes)
}

/** The move a tap on [position] makes with [selection], or null when the tap does nothing. */
fun moveForTap(state: GameState, player: Player, selection: CardSelection, position: BoardPosition): Move? {
    val tactical = selection.tacticalAction
    val selectedIndices = selection.indices
    val hand = state.handOf(player)

    return when {
        // Tactical: Pair Wild Place
        tactical is TacticalAction.PairWild -> {
            if (position in selection.placements) {
                Move.CraftPlace(player.id, tactical.card1, tactical.card2, position)
            } else null
        }
        // Tactical: Connector Snipe Remove
        tactical is TacticalAction.ConnectorSnipe -> {
            if (position in selection.snipes) {
                Move.CraftRemove(player.id, tactical.card1, tactical.card2, position)
            } else null
        }
        // Classic: Single Card Move
        selectedIndices.size == 1 -> {
            val card = hand[selectedIndices.first()]
            when {
                card.isOneEyedJack() -> {
                    if (position in selection.snipes) {
                        Move.Remove(player.id, card, position)
                    } else null
                }
                else -> {
                    if (position in selection.placements) {
                        Move.Place(player.id, card, position)
                    } else null
                }
            }
        }
        else -> null
    }
}
