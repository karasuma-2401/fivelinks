package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ClientMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameEndReason
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ServerMessage
import com.karasuma.fivelinks.fivelinks_cmp.protocol.viewFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class) // the test scheduler's time source
class OnlineGameControllerTest {

    private val connection = FakeConnection()

    /** A session showing [dealt] to [ME], and a controller for its game screen. */
    private fun TestScope.controllerFor(dealt: GameState): Pair<OnlineSession, OnlineGameController> {
        val session = OnlineSession(connection, backgroundScope, testScheduler.timeSource)
        session.start()
        connection.state.value = ConnectionState.Connected(ME)
        connection.incoming.tryEmit(ServerMessage.Welcome("w", "1.1", "Me", playerId = ME, sessionToken = "t"))
        connection.incoming.tryEmit(ServerMessage.GameSnapshot("g", "K7QP2M", dealt.board, dealt.viewFor(ME), 60_000))
        runCurrent()
        return session to OnlineGameController(session, backgroundScope)
    }

    @Test
    fun pickingACardAndACell_sendsThatMove() = runTest {
        val dealt = dealOnlineGame()
        val (_, controller) = controllerFor(dealt)
        val hand = dealt.hands.getValue(ME).cards
        val index = hand.indexOfFirst { !it.isJack() }
        val target = dealt.board.positionsOf(hand[index]).first()

        controller.onCardClick(index)
        runCurrent()
        assertEquals(setOf(index), controller.uiState.value.selectedCardIndices)
        assertTrue(target in controller.uiState.value.validPlacementPositions)

        controller.onCellClick(target)
        runCurrent()
        val sent = connection.sent.single() as ClientMessage.SubmitMove
        assertEquals(Move.Place(ME, hand[index], target), sent.move)
        assertEquals(0, sent.turnNumber)
        // Input waits for the server's answer, and the picked cards are put back.
        assertTrue(controller.uiState.value.isPassingTurn)
        assertTrue(controller.uiState.value.selectedCardIndices.isEmpty())
    }

    @Test
    fun onTheOpponentsTurn_cardsCannotBePicked() = runTest {
        val (_, controller) = controllerFor(dealOnlineGame(first = YOU))

        controller.onCardClick(0)
        runCurrent()

        assertTrue(controller.uiState.value.selectedCardIndices.isEmpty())
        assertEquals(YOU, controller.uiState.value.gameState.currentPlayer.id)
    }

    @Test
    fun onlineUiState_marksTheViewerAndExposesTheServerTurnDeadline() = runTest {
        val dealt = dealOnlineGame(first = YOU)
        val (_, controller) = controllerFor(dealt)

        val state = controller.uiState.value
        assertEquals(ME, state.localPlayerId)
        assertNotNull(state.turnEndsAt)
        assertFalse(state.canRestart)
        // GameScreen uses localPlayerId to keep this hand visible during the opponent's turn.
        assertEquals(dealt.hands.getValue(ME), state.gameState.hands.getValue(ME))
    }

    @Test
    fun withoutALink_cardsCannotBePicked() = runTest {
        val (_, controller) = controllerFor(dealOnlineGame())
        connection.state.value = ConnectionState.Reconnecting(attempt = 1, retryInMs = 1_000)
        runCurrent()

        controller.onCardClick(0)
        runCurrent()

        assertTrue(controller.uiState.value.selectedCardIndices.isEmpty())
    }

    @Test
    fun playingAgain_asksForARematch() = runTest {
        val (_, controller) = controllerFor(dealOnlineGame())

        controller.startNewGame()
        runCurrent()

        assertTrue(connection.sent.single() is ClientMessage.RequestRematch)
    }

    @Test
    fun gameOverResult_isExposedForForfeitOverlay() = runTest {
        val (_, controller) = controllerFor(dealOnlineGame())
        val result = GameResult(Team.BLUE, GameEndReason.FORFEIT_DISCONNECTED)

        connection.incoming.tryEmit(ServerMessage.GameOver("over", result))
        runCurrent()

        assertTrue(controller.uiState.value.isGameOver)
        assertEquals(result, controller.uiState.value.gameResult)
        assertTrue(controller.uiState.value.canRestart)
    }

    @Test
    fun reorderingHand_updatesCardValueAtBothOldAndNewPositions() = runTest {
        val dealt = dealOnlineGame()
        val (_, controller) = controllerFor(dealt)
        val originalHand = dealt.hands.getValue(ME).cards
        // Find two distinct non-jack cards at different indices
        val fromIdx = originalHand.indexOfFirst { !it.isJack() }
        val toIdx = originalHand.indices.first { it != fromIdx && !originalHand[it].isJack() && originalHand[it] != originalHand[fromIdx] }
        val movedCard = originalHand[fromIdx]

        controller.reorderHand(fromIdx, toIdx)
        runCurrent()

        val reorderedHand = controller.uiState.value.gameState.hands.getValue(ME).cards
        assertEquals(movedCard, reorderedHand[toIdx])
        val newCardAtOldSlot = reorderedHand[fromIdx]

        // Clicking the old slot (fromIdx) must select and play newCardAtOldSlot, NOT movedCard
        controller.onCardClick(fromIdx)
        runCurrent()
        val target = dealt.board.positionsOf(newCardAtOldSlot).first()
        assertTrue(target in controller.uiState.value.validPlacementPositions)

        controller.onCellClick(target)
        runCurrent()
        val sent = connection.sent.single() as ClientMessage.SubmitMove
        assertEquals(Move.Place(ME, newCardAtOldSlot, target), sent.move)
    }
}
