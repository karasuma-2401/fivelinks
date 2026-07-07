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
        }
        return Result.success(next)
    }
    fun advanceTurn (state: GameState): GameState {
        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
        return  state.copy(currentPlayerIndex = nextPlayerIndex, turnNumber = state.turnNumber + 1)
    }

    fun applyPlace(state: GameState, move: Move.Place): GameState {
        val player = state.players.first()
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

    fun legalMoves(state: GameState, playerId: PlayerId): List<Move> {
        if (state.winner != null) return emptyList()
        val player = state.players.first { it.id == playerId } ?: return emptyList()
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
                        val chip = state.chips.at(pos)
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
        return moves
    }
}