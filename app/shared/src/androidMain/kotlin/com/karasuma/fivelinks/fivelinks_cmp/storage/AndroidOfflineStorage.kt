package com.karasuma.fivelinks.fivelinks_cmp.storage

import android.content.Context

/** Android-local persistence for preferences and saved offline game. */
class AndroidOfflineStorage(context: Context) : OfflineStorage {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun loadPreferences(): OfflinePreferences {
        val raw = preferences.getString(KEY_PREFERENCES, null)
        return if (raw != null) OfflineSerializer.decodePreferences(raw) else OfflinePreferences()
    }

    override fun savePreferences(preferences: OfflinePreferences) {
        val raw = OfflineSerializer.encodePreferences(preferences)
        this.preferences.edit().putString(KEY_PREFERENCES, raw).apply()
    }

    override fun loadSavedGame(): SavedOfflineGame? {
        val raw = preferences.getString(KEY_SAVED_GAME, null) ?: return null
        return OfflineSerializer.decodeGame(raw)
    }

    override fun saveGame(game: SavedOfflineGame?) {
        this.preferences.edit().apply {
            if (game == null) remove(KEY_SAVED_GAME)
            else putString(KEY_SAVED_GAME, OfflineSerializer.encodeGame(game))
        }.apply()
    }

    override fun clearSavedGame() {
        preferences.edit().remove(KEY_SAVED_GAME).apply()
    }

    private companion object {
        const val FILE_NAME = "five_links_offline"
        const val KEY_PREFERENCES = "offline_preferences"
        const val KEY_SAVED_GAME = "saved_game"
    }
}

