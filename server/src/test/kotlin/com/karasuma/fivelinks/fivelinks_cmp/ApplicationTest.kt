package com.karasuma.fivelinks.fivelinks_cmp

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello, Ktor!", response.bodyAsText())
    }

    @Test
    fun testHealthEndpoints() = testApplication {
        application {
            module()
        }
        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertEquals("Health OK", health.bodyAsText())

        val liveness = client.get("/health/liveness")
        assertEquals(HttpStatusCode.OK, liveness.status)
        assertTrue(liveness.bodyAsText().contains("UP"))

        val readiness = client.get("/health/readiness")
        assertEquals(HttpStatusCode.OK, readiness.status)
        assertTrue(readiness.bodyAsText().contains("activeRooms"))
    }

    @Test
    fun testMetricsEndpoint() = testApplication {
        application {
            module()
        }
        val metrics = client.get("/metrics")
        assertEquals(HttpStatusCode.OK, metrics.status)
        val body = metrics.bodyAsText()
        assertTrue(body.contains("uptimeSeconds"))
        assertTrue(body.contains("jvmTotalMemoryMb"))
        assertTrue(body.contains("totalRoomsCreated"))
    }

    @Test
    fun testProtocolVersionEndpoint() = testApplication {
        application {
            module()
        }
        val version = client.get("/protocol/version")
        assertEquals(HttpStatusCode.OK, version.status)
        assertTrue(version.bodyAsText().contains("major"))
    }
}
