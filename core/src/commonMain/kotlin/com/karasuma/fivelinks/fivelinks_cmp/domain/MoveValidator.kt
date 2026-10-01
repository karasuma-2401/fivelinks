package com.karasuma.fivelinks.fivelinks_cmp.domain

object MoveValidator {
    fun validate(move: Move, state: GameState): Result<Unit> {
        if (state.isGameOver) return Result.failure(IllegalStateException("Game is over!"))

        val player = state.players.firstOrNull { it.id == move.playerId } ?: return Result.failure(IllegalStateException("Player not found!"))

        if (player.id != state.currentPlayer.id) return Result.failure(IllegalStateException("It's not your turn"))

        val hand = state.handOf(player)

        return when (move) {
            is Move.Place -> {
                if (move.card !in hand) return Result.failure(IllegalStateException("Card not in hand"))
                validatePlace(state, move, player)
            }
            is Move.Remove -> {
                if (move.card !in hand) return Result.failure(IllegalStateException("Card not in hand"))
                validateRemove(state, move, player)
            }
            is Move.SwapDeadCard -> {
                if (move.card !in hand) return Result.failure(IllegalStateException("Card not in hand"))
                validateSwapDeadCard(state, move, player)
            }
            is Move.CraftPlace -> validateCraftPlace(state, move, player)
            is Move.CraftRemove -> validateCraftRemove(state, move, player)
            is Move.DivineWipe -> validateDivineWipe(state, move, player)
        }
    }

    private fun validatePlace(state: GameState, move: Move.Place, player: Player): Result<Unit> {
        if (state.board.isCorner(move.position)) return Result.failure(IllegalStateException("Cannot place on a corner"))
        if (state.chips.at(move.position) != null) return Result.failure(IllegalStateException("Position is already occupied!"))

        if (move.card.isTwoEyedJack()) return Result.success(Unit)
        if (move.card.isOneEyedJack()) return Result.failure(IllegalStateException("Cannot place a one-eyed jack"))

        val boardCard = state.board.cardAt(move.position)
        if (boardCard != null && boardCard != move.card) return Result.failure(IllegalStateException("Cannot place a card that is not the same as the one on the board"))
        return Result.success(Unit)
    }

    private fun validateRemove(state: GameState, move: Move.Remove, player: Player): Result<Unit> {
        if (!move.card.isOneEyedJack()) return Result.failure(IllegalStateException("Remove requires a one-eyed Jack"))
        val chipTeam = state.chips.at(move.position) ?: return Result.failure(IllegalStateException("no chip at ${move.position}"))
        if (chipTeam == player.team) return Result.failure(IllegalStateException("cannot remove own team's chip"))
        val locked = state.completedSequence.any { it.positions.contains(move.position) }
        if (locked) return Result.failure(IllegalStateException("cannot remove a chip that is locked"))
        return Result.success(Unit)
    }

    private fun validateSwapDeadCard(state: GameState, move: Move.SwapDeadCard, player: Player): Result<Unit> {
        if (move.card.isJack()) return Result.failure(IllegalStateException("Jacks are never dead"))
        val positions = state.board.positionsOf(move.card)
        if (positions.isEmpty()) return Result.failure(IllegalStateException("card ${move.card} is not on the board"))
        if (positions.any { !state.chips.contains(it) }) return Result.failure(IllegalStateException("card ${move.card} is not dead"))
        if (state.deck.isEmpty()) return Result.failure(IllegalStateException("cannot swap: draw pile is empty"))
        return Result.success(Unit)
    }

    private fun validateCraftPlace(state: GameState, move: Move.CraftPlace, player: Player): Result<Unit> {
        if (!state.config.enableTacticalCrafting) {
            return Result.failure(IllegalStateException("Tactical crafting is disabled in this game mode"))
        }
        val hand = state.handOf(player)
        if (!hand.hasCards(listOf(move.card1, move.card2))) {
            return Result.failure(IllegalStateException("Player does not hold both crafting cards in hand"))
        }
        if (!TacticalPatterns.isPair(move.card1, move.card2)) {
            return Result.failure(IllegalStateException("CraftPlace requires two cards of the same rank (pair)"))
        }
        if (state.board.isCorner(move.position)) {
            return Result.failure(IllegalStateException("Cannot place on a corner"))
        }
        if (state.chips.at(move.position) != null) {
            return Result.failure(IllegalStateException("Position is already occupied!"))
        }
        return Result.success(Unit)
    }

    private fun validateCraftRemove(state: GameState, move: Move.CraftRemove, player: Player): Result<Unit> {
        if (!state.config.enableTacticalCrafting) {
            return Result.failure(IllegalStateException("Tactical crafting is disabled in this game mode"))
        }
        val hand = state.handOf(player)
        if (!hand.hasCards(listOf(move.card1, move.card2))) {
            return Result.failure(IllegalStateException("Player does not hold both crafting cards in hand"))
        }
        if (!TacticalPatterns.isSuitedConnector(move.card1, move.card2)) {
            return Result.failure(IllegalStateException("CraftRemove requires two suited connector cards"))
        }
        val chipTeam = state.chips.at(move.position) ?: return Result.failure(IllegalStateException("no chip at ${move.position}"))
        if (chipTeam == player.team) {
            return Result.failure(IllegalStateException("cannot remove own team's chip"))
        }
        val locked = state.completedSequence.any { it.positions.contains(move.position) }
        if (locked) {
            return Result.failure(IllegalStateException("cannot remove a chip that is locked"))
        }
        return Result.success(Unit)
    }

    private fun validateDivineWipe(state: GameState, move: Move.DivineWipe, player: Player): Result<Unit> {
        if (!state.config.enableTacticalCrafting) {
            return Result.failure(IllegalStateException("Tactical crafting is disabled in this game mode"))
        }
        if (move.cards.size != 5) {
            return Result.failure(IllegalStateException("DivineWipe requires exactly 5 cards"))
        }
        val hand = state.handOf(player)
        if (!hand.hasCards(move.cards)) {
            return Result.failure(IllegalStateException("Player does not hold all 5 cards in hand"))
        }
        if (!TacticalPatterns.isStraightFlush(move.cards)) {
            return Result.failure(IllegalStateException("DivineWipe requires a suited straight flush of 5 cards"))
        }
        return Result.success(Unit)
    }
}