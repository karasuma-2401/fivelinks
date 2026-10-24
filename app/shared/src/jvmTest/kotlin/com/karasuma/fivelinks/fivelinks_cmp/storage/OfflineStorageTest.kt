package com.karasuma.fivelinks.fivelinks_cmp.storage

import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.OpponentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineStorageTest {

    @Test
    fun preferences_roundtrip_preservesAllFields() {
        val prefs = OfflinePreferences(
            playerName = "Chiến Thần",
            isNightTheme = true,
            opponentType = OpponentType.LOCAL_PASS_AND_PLAY,
            aiDifficulty = Difficulty.HARD,
            enableTactical = false,
            sequenceToWin = 1,
            soundEnabled = false,
            vibrationEnabled = false,
            hasCompletedOnboarding = true,
        )

        val encoded = OfflineSerializer.encodePreferences(prefs)
        val decoded = OfflineSerializer.decodePreferences(encoded)

        assertEquals(prefs, decoded)
        assertEquals(OpponentType.LOCAL_PASS_AND_PLAY, decoded.toMenuSettings().opponentType)
        assertEquals(Difficulty.HARD, decoded.toMenuSettings().aiDifficulty)
        assertFalse(decoded.toMenuSettings().enableTactical)
        assertEquals(1, decoded.toMenuSettings().sequenceToWin)
    }

    @Test
    fun decodePreferences_invalidJson_fallsBackToDefaults() {
        val decoded = OfflineSerializer.decodePreferences("{ corrupt_json: true ")
        assertEquals(OfflinePreferences(), decoded)
    }

    @Test
    fun savedGame_roundtrip_preservesGameStateAndDifficulty() {
        val config = GameConfig.tactical(playerCount = 2, teams = 2, seed = 42L)
        val players = listOf(
            Player(id = "p1", name = "Player 1", team = Team.BLUE, isAi = false),
            Player(id = "p2", name = "AI Bot", team = Team.RED, isAi = true)
        )
        val initialGame = GameEngine.initialize(config, players)
        val savedGame = SavedOfflineGame(
            gameState = initialGame,
            aiDifficulty = Difficulty.HARD,
            elapsedSeconds = 120,
            timestamp = 1700000000L
        )

        val encoded = OfflineSerializer.encodeGame(savedGame)
        val decoded = OfflineSerializer.decodeGame(encoded)

        assertNotNull(decoded)
        assertEquals(Difficulty.HARD, decoded.aiDifficulty)
        assertEquals(120, decoded.elapsedSeconds)
        assertEquals(initialGame.currentPlayer.id, decoded.gameState.currentPlayer.id)
        assertEquals(initialGame.turnNumber, decoded.gameState.turnNumber)
        assertEquals(initialGame.board.cells.size, decoded.gameState.board.cells.size)
        assertEquals(initialGame.deck.cards.size, decoded.gameState.deck.cards.size)
    }

    @Test
    fun decodeGame_invalidJson_returnsNull() {
        val decoded = OfflineSerializer.decodeGame("not valid json")
        assertNull(decoded)
    }

    @Test
    fun inMemoryOfflineStorage_managesStateCorrectly() {
        val storage = InMemoryOfflineStorage()
        assertEquals(OfflinePreferences(), storage.loadPreferences())
        assertNull(storage.loadSavedGame())

        val updatedPrefs = OfflinePreferences(soundEnabled = false, hasCompletedOnboarding = true)
        storage.savePreferences(updatedPrefs)
        assertEquals(updatedPrefs, storage.loadPreferences())

        val config = GameConfig.forPlayer(playerCount = 2, teams = 2, seed = 99L)
        val players = listOf(
            Player(id = "p1", name = "Player 1", team = Team.BLUE, isAi = false),
            Player(id = "p2", name = "AI Bot", team = Team.RED, isAi = true)
        )
        val game = GameEngine.initialize(config, players)
        val saved = SavedOfflineGame(gameState = game, aiDifficulty = Difficulty.EASY)

        storage.saveGame(saved)
        assertEquals(saved, storage.loadSavedGame())

        storage.clearSavedGame()
        assertNull(storage.loadSavedGame())
    }
}
