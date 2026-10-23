package com.karasuma.fivelinks.fivelinks_cmp.online

import platform.Foundation.NSUserDefaults

/** iOS-local persistence for guest identity and the opaque reconnect token. */
class IosOnlineProfileStore : OnlineProfileStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun load(): OnlineProfile = OnlineProfile(
        playerName = defaults.stringForKey(KEY_PLAYER_NAME) ?: DEFAULT_PLAYER_NAME,
        sessionToken = defaults.stringForKey(KEY_SESSION_TOKEN),
    )

    override fun save(profile: OnlineProfile) {
        defaults.setObject(profile.playerName, forKey = KEY_PLAYER_NAME)
        if (profile.sessionToken == null) defaults.removeObjectForKey(KEY_SESSION_TOKEN)
        else defaults.setObject(profile.sessionToken, forKey = KEY_SESSION_TOKEN)
    }

    private companion object {
        const val KEY_PLAYER_NAME = "player_name"
        const val KEY_SESSION_TOKEN = "session_token"
    }
}
