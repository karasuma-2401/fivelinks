package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Operational and timing configuration for the game server. */
data class OnlineConfig(
    val port: Int = defaultPort(),
    val host: String = defaultHost(),
    /** A player who does not move in time passes. */
    val turnTime: Duration = defaultDurationSeconds("TURN_TIME_SECONDS", 60).seconds,
    /** A player who stays disconnected this long during a game forfeits it. */
    val reconnectGrace: Duration = defaultDurationSeconds("RECONNECT_GRACE_SECONDS", 60).seconds,
    /** A room nobody joins is closed after this long. */
    val waitingRoomTtl: Duration = defaultDurationMinutes("WAITING_ROOM_TTL_MINUTES", 10).minutes,
    /** A session with no socket and no room is forgotten after this long. */
    val idleSessionTtl: Duration = defaultDurationMinutes("IDLE_SESSION_TTL_MINUTES", 30).minutes,
    /** Maximum concurrent active rooms before new rooms are rejected. */
    val maxRooms: Int = defaultInt("MAX_ROOMS", 1000),
    /** Maximum concurrent sessions before new handshakes are rejected. */
    val maxSessions: Int = defaultInt("MAX_SESSIONS", 5000),
    /** Allowed WebSocket/HTTP Origin headers; empty or set containing "*" means allow all. */
    val allowedOrigins: Set<String> = defaultAllowedOrigins(),
    /** Rate limiting enabled. */
    val rateLimitEnabled: Boolean = defaultBoolean("RATE_LIMIT_ENABLED", true),
    /** Max messages per minute per connection. */
    val rateLimitPerMinute: Int = defaultInt("RATE_LIMIT_PER_MINUTE", 300),
    /** Max moves per second per player. */
    val moveRateLimitPerSecond: Int = defaultInt("MOVE_RATE_LIMIT_PER_SECOND", 30),
) {
    fun isOriginAllowed(origin: String?): Boolean {
        if (allowedOrigins.isEmpty() || "*" in allowedOrigins) return true
        if (origin == null) return true // Non-browser clients (native Android, desktop, iOS) don't send Origin
        val normalized = origin.trim().removeSuffix("/")
        return allowedOrigins.any { it.trim().removeSuffix("/").equals(normalized, ignoreCase = true) }
    }

    companion object {
        fun fromEnvironment(): OnlineConfig = OnlineConfig()

        private fun defaultPort(): Int =
            System.getenv("PORT")?.toIntOrNull()
                ?: System.getProperty("fivelinks.port")?.toIntOrNull()
                ?: 8080

        private fun defaultHost(): String =
            System.getenv("HOST")
                ?: System.getProperty("fivelinks.host")
                ?: "0.0.0.0"

        private fun defaultInt(key: String, fallback: Int): Int =
            System.getenv(key)?.toIntOrNull()
                ?: System.getProperty(key.lowercase().replace('_', '.'))?.toIntOrNull()
                ?: fallback

        private fun defaultBoolean(key: String, fallback: Boolean): Boolean =
            System.getenv(key)?.toBooleanStrictOrNull()
                ?: System.getProperty(key.lowercase().replace('_', '.'))?.toBooleanStrictOrNull()
                ?: fallback

        private fun defaultDurationSeconds(key: String, fallback: Long): Long =
            System.getenv(key)?.toLongOrNull()
                ?: System.getProperty(key.lowercase().replace('_', '.'))?.toLongOrNull()
                ?: fallback

        private fun defaultDurationMinutes(key: String, fallback: Long): Long =
            System.getenv(key)?.toLongOrNull()
                ?: System.getProperty(key.lowercase().replace('_', '.'))?.toLongOrNull()
                ?: fallback

        private fun defaultAllowedOrigins(): Set<String> {
            val raw = System.getenv("ALLOWED_ORIGINS")
                ?: System.getProperty("fivelinks.allowed.origins")
                ?: "*"
            return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
    }
}
