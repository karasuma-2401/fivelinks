package com.karasuma.fivelinks.fivelinks_cmp.storage

import kotlinx.browser.window

/** Browser-local Web Storage persistence for offline preferences and saved game. */
class WebOfflineStorage : OfflineStorage {
    override fun loadPreferences(): OfflinePreferences = runCatching {
        window.localStorage.getItem(KEY_PREFERENCES)?.let { OfflineSerializer.decodePreferences(it) }
    }.getOrNull() ?: OfflinePreferences()

    override fun savePreferences(preferences: OfflinePreferences) {
        runCatching {
            window.localStorage.setItem(KEY_PREFERENCES, OfflineSerializer.encodePreferences(preferences))
        }
    }

    override fun loadSavedGame(): SavedOfflineGame? = runCatching {
        window.localStorage.getItem(KEY_SAVED_GAME)?.let { OfflineSerializer.decodeGame(it) }
    }.getOrNull()

    override fun saveGame(game: SavedOfflineGame?) {
        runCatching {
            if (game == null) window.localStorage.removeItem(KEY_SAVED_GAME)
            else window.localStorage.setItem(KEY_SAVED_GAME, OfflineSerializer.encodeGame(game))
        }
    }

    override fun clearSavedGame() {
        runCatching {
            window.localStorage.removeItem(KEY_SAVED_GAME)
        }
    }

    private companion object {
        const val KEY_PREFERENCES = "five_links.offline.preferences"
        const val KEY_SAVED_GAME = "five_links.offline.saved_game"
    }
}

