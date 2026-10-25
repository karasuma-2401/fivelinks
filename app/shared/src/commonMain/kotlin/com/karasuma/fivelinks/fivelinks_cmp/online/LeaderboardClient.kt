package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.model.LeaderboardResponse
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerLeaderboardEntry
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncResponse
import com.karasuma.fivelinks.fivelinks_cmp.protocol.ProtocolJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class LeaderboardClient(
    private val httpClient: HttpClient = createGameHttpClient(),
    private val defaultHttpServerUrl: String = ServerConfig.DEV_URL
) {
    /** Pre-warms the server (especially Render free-tier instances) in the background on app launch. */
    suspend fun warmup(serverUrl: String = defaultHttpServerUrl) {
        val candidates = candidateBaseUrls(serverUrl)
        for (base in candidates) {
            runCatching {
                if (base.contains("localhost") || base.contains("127.0.0.1")) {
                    withTimeout(2.seconds) { httpClient.get("$base/health/liveness") }
                } else {
                    httpClient.get("$base/health/liveness")
                }
            }
        }
    }

    suspend fun getLeaderboard(
        playerId: String? = null,
        limit: Int = 50,
        serverUrl: String = defaultHttpServerUrl
    ): Result<LeaderboardResponse> = runWithFallback(serverUrl) { baseUrl ->
        val url = buildString {
            append("$baseUrl/api/leaderboard?limit=$limit")
            if (!playerId.isNullOrBlank()) append("&playerId=$playerId")
        }
        val response = httpClient.get(url)
        val json = response.bodyAsText()
        ProtocolJson.decodeFromString<LeaderboardResponse>(json)
    }

    suspend fun syncPlayerStats(
        req: PlayerSyncRequest,
        serverUrl: String = defaultHttpServerUrl
    ): Result<PlayerLeaderboardEntry> = runWithFallback(serverUrl) { baseUrl ->
        val response = httpClient.post("$baseUrl/api/player/sync") {
            contentType(ContentType.Application.Json)
            setBody(ProtocolJson.encodeToString(PlayerSyncRequest.serializer(), req))
        }
        val json = response.bodyAsText()
        val parsed = ProtocolJson.decodeFromString<PlayerSyncResponse>(json)
        parsed.updatedEntry
    }

    private fun candidateBaseUrls(serverUrl: String): List<String> {
        val primary = ServerConfig.httpBaseUrl(serverUrl)
        return listOf(primary, ServerConfig.CLOUD_HTTP_URL).distinct()
    }

    private suspend fun <T> runWithFallback(
        serverUrl: String,
        block: suspend (baseUrl: String) -> T
    ): Result<T> {
        var lastError: Throwable? = null
        for (baseUrl in candidateBaseUrls(serverUrl)) {
            val isLocal = baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")
            val maxAttempts = if (isLocal) 1 else 2
            for (attempt in 1..maxAttempts) {
                try {
                    val value = if (isLocal) {
                        withTimeout(2500) { block(baseUrl) }
                    } else {
                        if (attempt > 1) {
                            runCatching { httpClient.get("$baseUrl/health/liveness") }
                        }
                        block(baseUrl)
                    }
                    return Result.success(value)
                } catch (e: Throwable) {
                    lastError = e
                    if (!isLocal && attempt < maxAttempts) {
                        delay(1500)
                    }
                }
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Không thể kết nối máy chủ xếp hạng"))
    }
}

