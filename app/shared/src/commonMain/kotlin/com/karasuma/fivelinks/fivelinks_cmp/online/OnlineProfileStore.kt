package com.karasuma.fivelinks.fivelinks_cmp.online

/**
 * The small amount of client identity needed to resume a guest session. The token is opaque to
 * the app: only the game server can interpret it. It is deliberately separate from room state,
 * which always remains on the server.
 */
data class OnlineProfile(
    val playerName: String = DEFAULT_PLAYER_NAME,
    val sessionToken: String? = null,
)

interface OnlineProfileStore {
    fun load(): OnlineProfile

    fun save(profile: OnlineProfile)
}

/** Used by targets that have not supplied a persistent platform store yet. */
object InMemoryOnlineProfileStore : OnlineProfileStore {
    private var profile = OnlineProfile()

    override fun load(): OnlineProfile = profile

    override fun save(profile: OnlineProfile) {
        this.profile = profile
    }
}

const val DEFAULT_PLAYER_NAME = "Người chơi"
