package com.karasuma.fivelinks.fivelinks_cmp

import com.karasuma.fivelinks.fivelinks_cmp.online.DEFAULT_PLAYER_NAME
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineProfile
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineProfileStore
import kotlinx.browser.window

/**
 * Browser-local persistence for a guest name and opaque reconnect token.
 *
 * The token deliberately grants no account-level access. A future account system must use
 * HttpOnly secure cookies (issued by the server) instead of Web Storage for credentials.
 */
class WebOnlineProfileStore : OnlineProfileStore {
    override fun load(): OnlineProfile = runCatching {
        OnlineProfile(
            playerName = window.localStorage.getItem(KEY_PLAYER_NAME)
                ?.takeIf(String::isNotBlank)
                ?: DEFAULT_PLAYER_NAME,
            sessionToken = window.localStorage.getItem(KEY_SESSION_TOKEN),
        )
    }.getOrDefault(OnlineProfile())

    override fun save(profile: OnlineProfile) {
        runCatching {
            window.localStorage.setItem(KEY_PLAYER_NAME, profile.playerName)
            if (profile.sessionToken == null) window.localStorage.removeItem(KEY_SESSION_TOKEN)
            else window.localStorage.setItem(KEY_SESSION_TOKEN, profile.sessionToken)
        }
    }

    private companion object {
        const val KEY_PLAYER_NAME = "five_links.online.player_name"
        const val KEY_SESSION_TOKEN = "five_links.online.session_token"
    }
}
