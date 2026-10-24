package com.karasuma.fivelinks.fivelinks_cmp.storage

import platform.Foundation.NSUserDefaults

/** iOS-local persistence for preferences and saved offline game. */
class IosOfflineStorage : OfflineStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun loadPreferences(): OfflinePreferences {
        val raw = defaults.stringForKey(KEY_PREFERENCES)
        return if (raw != null) OfflineSerializer.decodePreferences(raw) else OfflinePreferences()
    }

    override fun savePreferences(preferences: OfflinePreferences) {
        val raw = OfflineSerializer.encodePreferences(preferences)
        defaults.setObject(raw, forKey = KEY_PREFERENCES)
    }

    override fun loadSavedGame(): SavedOfflineGame? {
        val raw = defaults.stringForKey(KEY_SAVED_GAME) ?: return null
        return OfflineSerializer.decodeGame(raw)
    }

    override fun saveGame(game: SavedOfflineGame?) {
        if (game == null) defaults.removeObjectForKey(KEY_SAVED_GAME)
        else defaults.setObject(OfflineSerializer.encodeGame(game), forKey = KEY_SAVED_GAME)
    }

    override fun clearSavedGame() {
        defaults.removeObjectForKey(KEY_SAVED_GAME)
    }

    private companion object {
        const val KEY_PREFERENCES = "five_links.offline.preferences"
        const val KEY_SAVED_GAME = "five_links.offline.saved_game"
    }
}

