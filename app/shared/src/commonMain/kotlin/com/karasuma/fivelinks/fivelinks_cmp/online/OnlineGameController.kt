package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.CardSelection
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameController
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameUiState
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.TacticalAction
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.cardSelection
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.moveForTap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

/**
 * Lets the regular game screen play an online game. Picking cards stays on this device;
 * a move becomes a request to the server, and the screen shows the game the server reports.
 * Create it once the session has a game.
 */
class OnlineGameController(
    private val session: OnlineSession,
    scope: CoroutineScope,
) : GameController {

    /** Cards picked during one turn; every turn starts with nothing picked. */
    private data class Picked(val turn: Int, val selection: CardSelection)

    private val picked = MutableStateFlow<Picked?>(null)
    private val customHandOrder = MutableStateFlow<List<com.karasuma.fivelinks.fivelinks_cmp.domain.Card>?>(null)

    override val uiState: StateFlow<GameUiState> = combine(session.state, picked, customHandOrder) { online, picked, customOrder ->
        online.toUiState(picked, customOrder)
    }
        .filterNotNull()
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            checkNotNull(session.state.value.toUiState(null, null)) { "The session has no game to show" },
        )

    override fun onCardClick(index: Int) {
        val game = playableGame() ?: return
        val state = effectiveState(game)
        val hand = state.handOf(me(game)).cards
        if (index !in hand.indices) return
        val picked = selectionFor(game).indices
        val indices = if (index in picked) picked - index else picked + index
        session.clearError()
        this.picked.value = Picked(game.view.turnNumber, cardSelection(state, me(game), indices))
    }

    override fun onCellClick(position: BoardPosition) {
        val game = playableGame() ?: return
        val state = effectiveState(game)
        val move = moveForTap(state, me(game), selectionFor(game), position) ?: return
        play(move)
    }

    override fun triggerDivineWipe() {
        val game = playableGame() ?: return
        val wipe = selectionFor(game).tacticalAction as? TacticalAction.DivineWipe ?: return
        play(Move.DivineWipe(game.view.viewerId, wipe.cards))
    }

    override fun swapDeadCard(index: Int) {
        val game = playableGame() ?: return
        val state = effectiveState(game)
        val card = state.handOf(me(game)).cards.getOrNull(index) ?: return
        play(Move.SwapDeadCard(game.view.viewerId, card))
    }

    override fun reorderHand(fromIndex: Int, toIndex: Int) {
        val game = session.state.value.game ?: return
        val myId = game.view.viewerId
        val currentCards = effectiveHandCards(game.state.hands[myId]?.cards.orEmpty(), customHandOrder.value).toMutableList()
        if (fromIndex in currentCards.indices && toIndex in currentCards.indices && fromIndex != toIndex) {
            val moved = currentCards.removeAt(fromIndex)
            currentCards.add(toIndex, moved)
            customHandOrder.value = currentCards
            val stateAfterReorder = effectiveState(game, currentCards)
            val currentPicked = picked.value
            if (currentPicked != null) {
                val newIndices = currentPicked.selection.indices.map { idx ->
                    when {
                        idx == fromIndex -> toIndex
                        fromIndex < toIndex && idx in (fromIndex + 1)..toIndex -> idx - 1
                        fromIndex > toIndex && idx in toIndex until fromIndex -> idx + 1
                        else -> idx
                    }
                }.toSet()
                picked.value = Picked(currentPicked.turn, cardSelection(stateAfterReorder, me(game), newIndices))
            }
        }
    }

    /** Online there is no restart: this asks the opponent for a rematch. */
    override fun startNewGame() = session.requestRematch()

    private fun play(move: Move) {
        picked.value = null
        session.submitMove(move)
    }

    /** The game, when it is this player's turn and they may act now. */
    private fun playableGame(): OnlineGame? {
        val online = session.state.value
        val game = online.game ?: return null
        val canAct = game.isMyTurn && !game.awaitingMove && online.result == null &&
            online.connection is ConnectionState.Connected
        return game.takeIf { canAct }
    }

    private fun selectionFor(game: OnlineGame): CardSelection =
        picked.value?.takeIf { it.turn == game.view.turnNumber && game.isMyTurn }?.selection ?: CardSelection()

    private fun me(game: OnlineGame): Player = game.view.players.first { it.id == game.view.viewerId }

    private fun effectiveHandCards(
        baseHand: List<com.karasuma.fivelinks.fivelinks_cmp.domain.Card>,
        customOrder: List<com.karasuma.fivelinks.fivelinks_cmp.domain.Card>?,
    ): List<com.karasuma.fivelinks.fivelinks_cmp.domain.Card> {
        if (customOrder == null || baseHand.isEmpty()) return baseHand
        val remaining = baseHand.toMutableList()
        val ordered = ArrayList<com.karasuma.fivelinks.fivelinks_cmp.domain.Card>(baseHand.size)
        for (card in customOrder) {
            if (remaining.remove(card)) {
                ordered.add(card)
            }
        }
        ordered.addAll(remaining)
        return ordered
    }

    private fun effectiveState(
        game: OnlineGame,
        customOrder: List<com.karasuma.fivelinks.fivelinks_cmp.domain.Card>? = customHandOrder.value,
    ): com.karasuma.fivelinks.fivelinks_cmp.domain.GameState {
        val myId = game.view.viewerId
        val baseHand = game.state.hands[myId]?.cards ?: return game.state
        val ordered = effectiveHandCards(baseHand, customOrder)
        if (ordered == baseHand) return game.state
        val newHands = game.state.hands.toMutableMap()
        newHands[myId] = com.karasuma.fivelinks.fivelinks_cmp.domain.Hand(ordered)
        return game.state.copy(hands = newHands)
    }

    private fun OnlineState.toUiState(picked: Picked?, customOrder: List<com.karasuma.fivelinks.fivelinks_cmp.domain.Card>?): GameUiState? {
        val game = game ?: return null
        val effectiveState = effectiveState(game, customOrder)
        val selection = picked?.takeIf { it.turn == game.view.turnNumber && game.isMyTurn }?.selection
            ?: CardSelection()
        return GameUiState(
            gameState = effectiveState,
            selectedCardIndices = selection.indices,
            activeTacticalAction = selection.tacticalAction,
            validPlacementPositions = selection.placements,
            validSnipePositions = selection.snipes,
            // The screen holds input while a move is on its way to the server.
            isPassingTurn = game.awaitingMove,
            errorMessage = error,
            isGameOver = game.view.isGameOver || result != null,
            localPlayerId = game.view.viewerId,
            gameResult = result,
            turnEndsAt = game.turnEndsAt,
            canRestart = result != null,
            isRematchPending = room?.players
                ?.firstOrNull { it.playerId == game.view.viewerId }
                ?.wantsRematch == true,
            opponentDisconnected = opponentAway,
            opponentForfeitAt = opponentForfeitAt,
        )
    }
}
