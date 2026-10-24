package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class ServerStatusPayload(
    val status: String,
    val uptimeSeconds: Long,
    val activeRooms: Int,
    val activeSessions: Int,
    val maxRooms: Int,
    val maxSessions: Int,
    val isShuttingDown: Boolean,
)

@Serializable
data class ServerMetricsPayload(
    val status: String,
    val uptimeSeconds: Long,
    val activeRooms: Int,
    val activeSessions: Int,
    val maxRooms: Int,
    val maxSessions: Int,
    val totalRoomsCreated: Long,
    val totalGamesFinished: Long,
    val totalMovesSubmitted: Long,
    val totalRateLimitHits: Long,
    val isShuttingDown: Boolean,
    val jvmFreeMemoryMb: Long,
    val jvmTotalMemoryMb: Long,
    val jvmMaxMemoryMb: Long,
    val availableProcessors: Int,
)

class ServerMetrics {
    val startTimeMillis: Long = System.currentTimeMillis()
    val totalRoomsCreated = AtomicLong(0)
    val totalGamesFinished = AtomicLong(0)
    val totalMovesSubmitted = AtomicLong(0)
    val totalRateLimitHits = AtomicLong(0)

    val uptimeSeconds: Long
        get() = (System.currentTimeMillis() - startTimeMillis) / 1000L

    fun toStatus(activeRooms: Int, activeSessions: Int, maxRooms: Int, maxSessions: Int, isShuttingDown: Boolean): ServerStatusPayload {
        return ServerStatusPayload(
            status = if (isShuttingDown) "SHUTTING_DOWN" else "UP",
            uptimeSeconds = uptimeSeconds,
            activeRooms = activeRooms,
            activeSessions = activeSessions,
            maxRooms = maxRooms,
            maxSessions = maxSessions,
            isShuttingDown = isShuttingDown,
        )
    }

    fun toMetrics(activeRooms: Int, activeSessions: Int, maxRooms: Int, maxSessions: Int, isShuttingDown: Boolean): ServerMetricsPayload {
        val runtime = Runtime.getRuntime()
        val mb = 1024 * 1024
        return ServerMetricsPayload(
            status = if (isShuttingDown) "SHUTTING_DOWN" else "UP",
            uptimeSeconds = uptimeSeconds,
            activeRooms = activeRooms,
            activeSessions = activeSessions,
            maxRooms = maxRooms,
            maxSessions = maxSessions,
            totalRoomsCreated = totalRoomsCreated.get(),
            totalGamesFinished = totalGamesFinished.get(),
            totalMovesSubmitted = totalMovesSubmitted.get(),
            totalRateLimitHits = totalRateLimitHits.get(),
            isShuttingDown = isShuttingDown,
            jvmFreeMemoryMb = runtime.freeMemory() / mb,
            jvmTotalMemoryMb = runtime.totalMemory() / mb,
            jvmMaxMemoryMb = runtime.maxMemory() / mb,
            availableProcessors = runtime.availableProcessors(),
        )
    }
}

