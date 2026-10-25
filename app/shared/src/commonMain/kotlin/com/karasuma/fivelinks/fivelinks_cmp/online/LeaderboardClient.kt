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

class LeaderboardClient(
    private val httpClient: HttpClient = createGameHttpClient(),
    private val defaultHttpServerUrl: String = "https://fivelinks-server.onrender.com"
) {
    suspend fun getLeaderboard(
        playerId: String? = null,
        limit: Int = 50,
        serverUrl: String = defaultHttpServerUrl
    ): Result<LeaderboardResponse> = runCatching {
        val baseUrl = ServerConfig.httpBaseUrl(serverUrl)
        val url = buildString {
            append("$baseUrl/api/leaderboard?limit=$limit")
            if (playerId != null) append("&playerId=$playerId")
        }
        val response = httpClient.get(url)
        val json = response.bodyAsText()
        ProtocolJson.decodeFromString<LeaderboardResponse>(json)
    }

    suspend fun syncPlayerStats(
        req: PlayerSyncRequest,
        serverUrl: String = defaultHttpServerUrl
    ): Result<PlayerLeaderboardEntry> = runCatching {
        val baseUrl = ServerConfig.httpBaseUrl(serverUrl)
        val response = httpClient.post("$baseUrl/api/player/sync") {
            contentType(ContentType.Application.Json)
            setBody(ProtocolJson.encodeToString(PlayerSyncRequest.serializer(), req))
        }
        val json = response.bodyAsText()
        val parsed = ProtocolJson.decodeFromString<PlayerSyncResponse>(json)
        parsed.updatedEntry
    }
}

