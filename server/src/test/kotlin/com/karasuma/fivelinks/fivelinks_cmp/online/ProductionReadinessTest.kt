package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionReadinessTest {

    @Test
    fun onlineConfig_defaultValues_areSound() {
        val config = OnlineConfig()
        assertTrue(config.port > 0)
        assertEquals("0.0.0.0", config.host)
        assertEquals(1000, config.maxRooms)
        assertEquals(5000, config.maxSessions)
        assertTrue(config.rateLimitEnabled)
        assertTrue(config.isOriginAllowed("http://localhost:8080"))
        assertTrue(config.isOriginAllowed(null))
    }

    @Test
    fun onlineConfig_originFiltering_worksAsExpected() {
        val config = OnlineConfig(allowedOrigins = setOf("https://fivelinks.app", "http://localhost:3000"))
        assertTrue(config.isOriginAllowed(null)) // Native client
        assertTrue(config.isOriginAllowed("https://fivelinks.app"))
        assertTrue(config.isOriginAllowed("https://fivelinks.app/"))
        assertTrue(config.isOriginAllowed("http://localhost:3000"))
        assertFalse(config.isOriginAllowed("https://evil-site.com"))
    }

    @Test
    fun rateLimiter_allowsUpToLimit_thenRejects() {
        val limiter = RateLimiter(limit = 3, windowMillis = 10_000L)
        val key = "test-client"

        assertTrue(limiter.tryAcquire(key))
        assertTrue(limiter.tryAcquire(key))
        assertTrue(limiter.tryAcquire(key))
        assertFalse(limiter.tryAcquire(key)) // 4th attempt should be rejected

        // Different key is unaffected
        assertTrue(limiter.tryAcquire("other-client"))
    }

    @Test
    fun serverMetrics_incrementsAndTracksCounters() {
        val metrics = ServerMetrics()
        assertEquals(0, metrics.totalRoomsCreated.get())
        assertEquals(0, metrics.totalMovesSubmitted.get())
        assertEquals(0, metrics.totalGamesFinished.get())

        metrics.totalRoomsCreated.incrementAndGet()
        metrics.totalMovesSubmitted.addAndGet(5)
        metrics.totalGamesFinished.incrementAndGet()

        val status = metrics.toStatus(activeRooms = 1, activeSessions = 2, maxRooms = 10, maxSessions = 50, isShuttingDown = false)
        assertEquals("UP", status.status)
        assertEquals(1, status.activeRooms)
        assertEquals(2, status.activeSessions)
        assertFalse(status.isShuttingDown)

        val report = metrics.toMetrics(activeRooms = 1, activeSessions = 2, maxRooms = 10, maxSessions = 50, isShuttingDown = false)
        assertEquals(1L, report.totalRoomsCreated)
        assertEquals(5L, report.totalMovesSubmitted)
        assertEquals(1L, report.totalGamesFinished)
        assertTrue(report.jvmTotalMemoryMb > 0)
    }
}

