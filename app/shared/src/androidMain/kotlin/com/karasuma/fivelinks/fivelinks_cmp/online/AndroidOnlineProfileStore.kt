package com.karasuma.fivelinks.fivelinks_cmp.online

import android.content.Context

/**
 * Android's app-private storage for the guest name and opaque reconnect token. This data is not
 * shared with other apps; a future account system should move credentials to Android Keystore.
 */
class AndroidOnlineProfileStore(context: Context) : OnlineProfileStore {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun load(): OnlineProfile = OnlineProfile(
        playerName = preferences.getString(KEY_PLAYER_NAME, DEFAULT_PLAYER_NAME) ?: DEFAULT_PLAYER_NAME,
        sessionToken = preferences.getString(KEY_SESSION_TOKEN, null),
    )

    override fun save(profile: OnlineProfile) {
        preferences.edit().apply {
            putString(KEY_PLAYER_NAME, profile.playerName)
            if (profile.sessionToken == null) remove(KEY_SESSION_TOKEN)
            else putString(KEY_SESSION_TOKEN, profile.sessionToken)
        }.apply()
    }

    private companion object {
        const val FILE_NAME = "five_links_online"
        const val KEY_PLAYER_NAME = "player_name"
        const val KEY_SESSION_TOKEN = "session_token"
    }
}
