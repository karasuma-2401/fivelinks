package com.karasuma.fivelinks.fivelinks_cmp.storage

import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.online.DEFAULT_PLAYER_NAME
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuSettings
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.OpponentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OfflinePreferences(
    val playerName: String = DEFAULT_PLAYER_NAME,
    val isNightTheme: Boolean? = null,
    val opponentType: OpponentType = OpponentType.AI,
    val aiDifficulty: Difficulty = Difficulty.MEDIUM,
    val enableTactical: Boolean = true,
    val sequenceToWin: Int = 2,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
) {
    fun toMenuSettings(): MenuSettings = MenuSettings(
        opponentType = opponentType,
        aiDifficulty = aiDifficulty,
        enableTactical = enableTactical,
        sequenceToWin = sequenceToWin,
    )
}

@Serializable
data class SavedOfflineGame(
    val gameState: GameState,
    val aiDifficulty: Difficulty,
    val elapsedSeconds: Int = 0,
    val timestamp: Long = 0L,
)

interface OfflineStorage {
    fun loadPreferences(): OfflinePreferences
    fun savePreferences(preferences: OfflinePreferences)
    fun loadSavedGame(): SavedOfflineGame?
    fun saveGame(game: SavedOfflineGame?)
    fun clearSavedGame()
}

class InMemoryOfflineStorage : OfflineStorage {
    private var preferences = OfflinePreferences()
    private var savedGame: SavedOfflineGame? = null

    override fun loadPreferences(): OfflinePreferences = preferences

    override fun savePreferences(preferences: OfflinePreferences) {
        this.preferences = preferences
    }

    override fun loadSavedGame(): SavedOfflineGame? = savedGame

    override fun saveGame(game: SavedOfflineGame?) {
        this.savedGame = game
    }

    override fun clearSavedGame() {
        this.savedGame = null
    }
}

object OfflineSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodePreferences(preferences: OfflinePreferences): String = json.encodeToString(preferences)

    fun decodePreferences(raw: String): OfflinePreferences = runCatching {
        json.decodeFromString<OfflinePreferences>(raw)
    }.getOrDefault(OfflinePreferences())

    fun encodeGame(game: SavedOfflineGame): String = json.encodeToString(game)

    fun decodeGame(raw: String): SavedOfflineGame? = runCatching {
        json.decodeFromString<SavedOfflineGame>(raw)
    }.getOrNull()
}

