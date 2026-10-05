package com.karasuma.fivelinks.fivelinks_cmp.domain

object GameEngine {
    fun initialize (
        config: GameConfig,
        players: List<Player>
    ): GameState {
        require(players.size == config.playerCount) { "Invalid number of players"}
        require(players.toSet().size == players.size) { "Players must be unique" }
        var deck: Deck = Deck.twoShuffleDeck(config.seed)
        val hands = mutableMapOf<PlayerId, Hand>()

        for (player in players) {
            val drawn = mutableListOf<Card>()
            repeat(config.handSize) {
                val (card, restOfDeck) = deck.draw()
                card?.let { drawn.add(it) }
                deck = restOfDeck
            }
            hands[player.id] = Hand(drawn)
        }
        return GameState(
            config = config,
            hands = hands,
            board = Board.standard,
            chips = ChipMap(),
            deck = deck,
            discard = emptyList(),
            players = players,
            currentPlayerIndex = 0
        )
    }

    fun applyMove(state: GameState, move: Move): Result<GameState> {
        val validation = MoveValidator.validate(move, state)
        if (validation.isFailure) return Result.failure(validation.exceptionOrNull()!!)

        val next = when(move) {
            is Move.Place -> applyPlace(state, move)
            is Move.Remove -> applyRemove(state, move)
            is Move.SwapDeadCard -> applySwapDeadCard(state, move)
            is Move.CraftPlace -> applyCraftPlace(state, move)
            is Move.CraftRemove -> applyCraftRemove(state, move)
            is Move.DivineWipe -> applyDivineWipe(state, move)
        }
        return Result.success(next)
    }

    fun advanceTurn(state: GameState): GameState {
        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
        return state.copy(currentPlayerIndex = nextPlayerIndex, turnNumber = state.turnNumber + 1)
    }

    /**
     * Late in a game the deck can run dry and leave the player to move with nothing
     * playable. They pass; if nobody can play any more, the game ends in a draw.
     * Call it after every turn change (moves, timeouts) and at the start of a game.
     */
    fun skipStuckTurns(state: GameState): SkippedTurns {
        if (state.isGameOver) return SkippedTurns(state, emptyList())
        var current = state
        val passed = mutableListOf<PlayerId>()
        repeat(state.players.size) {
            if (legalMoves(current, current.currentPlayer.id).isNotEmpty()) return SkippedTurns(current, passed)
            passed += current.currentPlayer.id
            current = advanceTurn(current)
        }
        return SkippedTurns(current.copy(isDraw = true), passed)
    }

    fun applyPlace(state: GameState, move: Move.Place): GameState {
        val player = state.players.first { it.id == move.playerId }
        val chipsAfter = state.chips.place(move.position, player.team)
        val handAfter = state.handOf(player).without(move.card)
        val (card, deck) = state.deck.draw()
        val handFinal = if (card != null) handAfter.with(card) else handAfter
        val placed = state.copy(
            chips = chipsAfter,
            deck = deck,
            hands = state.hands.plus(player.id to handFinal),
            lastMove = move,
            discard = state.discard.plus(move.card)
        )
        val newSeqs = SequenceDetector.findSequence(placed, player.team)
        val withSeqs = if (newSeqs.isEmpty()) placed else placed.copy(completedSequence = placed.completedSequence.plus(newSeqs))

        val winner = WinDetector.detect(withSeqs)
        val finalState = if (winner != null) withSeqs.copy(winner = winner) else withSeqs

        return if (winner != null) finalState else advanceTurn(finalState)
    }

    fun applyRemove(state: GameState, move: Move.Remove): GameState {
        val player = state.players.first { it.id == move.playerId }
        val chipsAfter = state.chips.remove(move.position)
        val handAfter = state.handOf(player).without(move.card)
        val (card, deck) = state.deck.draw()
        val handFinal = if (card != null) handAfter.with(card) else handAfter

        val removed = state.copy(
            hands = state.hands.plus(player.id to handFinal),
            chips = chipsAfter,
            deck = deck,
            discard = state.discard.plus(move.card),
            lastMove = move,
        )
        return advanceTurn(removed)
    }

    fun applySwapDeadCard(state: GameState, move: Move.SwapDeadCard): GameState {
        val player = state.players.first { it.id == move.playerId}
        val handAfter = state.handOf(player).without(move.card)
        val (card, deck) = state.deck.draw()
        val handFinal = if (card != null) handAfter.with(card) else handAfter

        val swapped = state.copy(
            hands = state.hands.plus(player.id to handFinal),
            deck = deck,
            lastMove = move,
            discard = state.discard.plus(move.card)
        )
        return advanceTurn(swapped)
    }

    fun applyCraftPlace(state: GameState, move: Move.CraftPlace): GameState {
        val player = state.players.first { it.id == move.playerId }
        val chipsAfter = state.chips.place(move.position, player.team)
        val cardsToDiscard = listOf(move.card1, move.card2)
        val handAfter = state.handOf(player).withoutAll(cardsToDiscard)
        val (drawnCards, deckAfter) = drawCards(state.deck, cardsToDiscard.size)
        val handFinal = handAfter.withAll(drawnCards)

        val placed = state.copy(
            chips = chipsAfter,
            deck = deckAfter,
            hands = state.hands.plus(player.id to handFinal),
            lastMove = move,
            discard = state.discard.plus(cardsToDiscard)
        )
        val newSeqs = SequenceDetector.findSequence(placed, player.team)
        val withSeqs = if (newSeqs.isEmpty()) placed else placed.copy(completedSequence = placed.completedSequence.plus(newSeqs))

        val winner = WinDetector.detect(withSeqs)
        val finalState = if (winner != null) withSeqs.copy(winner = winner) else withSeqs

        return if (winner != null) finalState else advanceTurn(finalState)
    }

    fun applyCraftRemove(state: GameState, move: Move.CraftRemove): GameState {
        val player = state.players.first { it.id == move.playerId }
        val chipsAfter = state.chips.remove(move.position)
        val cardsToDiscard = listOf(move.card1, move.card2)
        val handAfter = state.handOf(player).withoutAll(cardsToDiscard)
        val (drawnCards, deckAfter) = drawCards(state.deck, cardsToDiscard.size)
        val handFinal = handAfter.withAll(drawnCards)

        val removed = state.copy(
            hands = state.hands.plus(player.id to handFinal),
            chips = chipsAfter,
            deck = deckAfter,
            discard = state.discard.plus(cardsToDiscard),
            lastMove = move
        )
        return advanceTurn(removed)
    }

    fun applyDivineWipe(state: GameState, move: Move.DivineWipe): GameState {
        val player = state.players.first { it.id == move.playerId }
        val chipsAfter = state.chips.keepOnlyTeam(player.team)
        val sequencesAfter = state.completedSequence.filter { it.team == player.team }
        val handAfter = state.handOf(player).withoutAll(move.cards)
        val (drawnCards, deckAfter) = drawCards(state.deck, move.cards.size)
        val handFinal = handAfter.withAll(drawnCards)

        val wiped = state.copy(
            hands = state.hands.plus(player.id to handFinal),
            chips = chipsAfter,
            completedSequence = sequencesAfter,
            deck = deckAfter,
            discard = state.discard.plus(move.cards),
            lastMove = move
        )
        return advanceTurn(wiped)
    }

    private fun drawCards(deck: Deck, count: Int): Pair<List<Card>, Deck> {
        var currentDeck = deck
        val drawn = mutableListOf<Card>()
        repeat(count) {
            val (card, nextDeck) = currentDeck.draw()
            if (card != null) {
                drawn.add(card)
                currentDeck = nextDeck
            }
        }
        return drawn to currentDeck
    }

    fun legalMoves(state: GameState, playerId: PlayerId): List<Move> {
        if (state.isGameOver) return emptyList()
        val player = state.players.firstOrNull { it.id == playerId } ?: return emptyList()
        if (state.currentPlayer.id != playerId) return emptyList()

        val moves = mutableListOf<Move>()
        for (card in state.handOf(player).cards.distinct()) {
            when {
                card.isTwoEyedJack() -> {
                    for (pos in BoardPosition.all) {
                        if (state.board.isCorner(pos)) continue
                        if (state.chips.contains(pos)) continue

                        moves += Move.Place(player.id, card, pos)
                    }
                }
                card.isOneEyedJack() -> {
                    for (pos in BoardPosition.all) {
                        val chip = state.chips.at(pos) ?: continue
                        if (chip == player.team) continue
                        if (state.completedSequence.any { pos in it.positions }) continue

                        moves += Move.Remove(player.id, card, pos)
                    }
                }
                else -> {
                    val positions = state.board.positionsOf(card)
                    val openPositions = positions.filter { !state.chips.contains(it) }
                    for (pos in openPositions)
                        moves += Move.Place(player.id, card, pos)

                    if (positions.isNotEmpty() && openPositions.isEmpty() && !state.deck.isEmpty())
                        moves += Move.SwapDeadCard(player.id, card)
                }
            }
        }

        if (state.config.enableTacticalCrafting) {
            val hand = state.handOf(player)
            // Tactical: Straight Flush (Divine Wipe)
            val sf = TacticalPatterns.findStraightFlush(hand)
            if (sf != null) {
                moves += Move.DivineWipe(player.id, sf)
            }

            // Tactical: Pairs -> CraftPlace
            val pairs = TacticalPatterns.findPairs(hand)
            if (pairs.isNotEmpty()) {
                val openPositions = BoardPosition.all.filter { pos ->
                    !state.board.isCorner(pos) && !state.chips.contains(pos)
                }
                for (pair in pairs) {
                    for (pos in openPositions) {
                        moves += Move.CraftPlace(player.id, pair.first, pair.second, pos)
                    }
                }
            }

            // Tactical: Suited Connectors -> CraftRemove
            val connectors = TacticalPatterns.findSuitedConnectors(hand)
            if (connectors.isNotEmpty()) {
                val removablePositions = BoardPosition.all.filter { pos ->
                    val chip = state.chips.at(pos)
                    chip != null && chip != player.team && state.completedSequence.none { pos in it.positions }
                }
                for (conn in connectors) {
                    for (pos in removablePositions) {
                        moves += Move.CraftRemove(player.id, conn.first, conn.second, pos)
                    }
                }
            }
        }

        return moves
    }
}

/** Result of [GameEngine.skipStuckTurns]: the state to play on and who had to pass, in order. */
data class SkippedTurns(val state: GameState, val passed: List<PlayerId>)
