package com.karasuma.fivelinks.fivelinks_cmp.storage

import java.util.prefs.Preferences

/** Desktop-local persistence for preferences and saved offline game. */
class JvmOfflineStorage : OfflineStorage {
    private val preferences = Preferences.userRoot().node("com/karasuma/fivelinks/offline")

    override fun loadPreferences(): OfflinePreferences {
        val raw = preferences.get(KEY_PREFERENCES, null)
        return if (raw != null) OfflineSerializer.decodePreferences(raw) else OfflinePreferences()
    }

    override fun savePreferences(preferences: OfflinePreferences) {
        val raw = OfflineSerializer.encodePreferences(preferences)
        this.preferences.put(KEY_PREFERENCES, raw)
    }

    override fun loadSavedGame(): SavedOfflineGame? {
        val raw = preferences.get(KEY_SAVED_GAME, null) ?: return null
        return OfflineSerializer.decodeGame(raw)
    }

    override fun saveGame(game: SavedOfflineGame?) {
        if (game == null) {
            preferences.remove(KEY_SAVED_GAME)
        } else {
            val raw = OfflineSerializer.encodeGame(game)
            preferences.put(KEY_SAVED_GAME, raw)
        }
    }

    override fun clearSavedGame() {
        preferences.remove(KEY_SAVED_GAME)
    }

    private companion object {
        const val KEY_PREFERENCES = "offline_preferences"
        const val KEY_SAVED_GAME = "saved_game"
    }
}

