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

    override val uiState: StateFlow<GameUiState> = combine(session.state, picked) { online, picked ->
        online.toUiState(picked)
    }
        .filterNotNull()
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            checkNotNull(session.state.value.toUiState(null)) { "The session has no game to show" },
        )

    override fun onCardClick(index: Int) {
        val game = playableGame() ?: return
        if (index !in game.view.hand.cards.indices) return
        val picked = selectionFor(game).indices
        val indices = if (index in picked) picked - index else picked + index
        session.clearError()
        this.picked.value = Picked(game.view.turnNumber, cardSelection(game.state, me(game), indices))
    }

    override fun onCellClick(position: BoardPosition) {
        val game = playableGame() ?: return
        val move = moveForTap(game.state, me(game), selectionFor(game), position) ?: return
        play(move)
    }

    override fun triggerDivineWipe() {
        val game = playableGame() ?: return
        val wipe = selectionFor(game).tacticalAction as? TacticalAction.DivineWipe ?: return
        play(Move.DivineWipe(game.view.viewerId, wipe.cards))
    }

    override fun swapDeadCard(index: Int) {
        val game = playableGame() ?: return
        val card = game.view.hand.cards.getOrNull(index) ?: return
        play(Move.SwapDeadCard(game.view.viewerId, card))
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

    private fun OnlineState.toUiState(picked: Picked?): GameUiState? {
        val game = game ?: return null
        val selection = picked?.takeIf { it.turn == game.view.turnNumber && game.isMyTurn }?.selection
            ?: CardSelection()
        return GameUiState(
            gameState = game.state,
            selectedCardIndices = selection.indices,
            activeTacticalAction = selection.tacticalAction,
            validPlacementPositions = selection.placements,
            validSnipePositions = selection.snipes,
            // The screen holds input while a move is on its way to the server.
            isPassingTurn = game.awaitingMove,
            errorMessage = error,
            isGameOver = game.view.isGameOver || result != null,
        )
    }
}
