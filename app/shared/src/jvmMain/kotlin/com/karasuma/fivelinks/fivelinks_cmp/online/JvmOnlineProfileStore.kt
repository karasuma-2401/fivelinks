package com.karasuma.fivelinks.fivelinks_cmp.online

import java.util.prefs.Preferences

/** Desktop-local persistence for the guest profile and opaque reconnect token. */
class JvmOnlineProfileStore : OnlineProfileStore {
    private val preferences = Preferences.userRoot().node("com/karasuma/fivelinks/online")

    override fun load(): OnlineProfile = OnlineProfile(
        playerName = preferences.get(KEY_PLAYER_NAME, DEFAULT_PLAYER_NAME),
        sessionToken = preferences.get(KEY_SESSION_TOKEN, null),
    )

    override fun save(profile: OnlineProfile) {
        preferences.put(KEY_PLAYER_NAME, profile.playerName)
        if (profile.sessionToken == null) preferences.remove(KEY_SESSION_TOKEN)
        else preferences.put(KEY_SESSION_TOKEN, profile.sessionToken)
    }

    private companion object {
        const val KEY_PLAYER_NAME = "player_name"
        const val KEY_SESSION_TOKEN = "session_token"
    }
}
