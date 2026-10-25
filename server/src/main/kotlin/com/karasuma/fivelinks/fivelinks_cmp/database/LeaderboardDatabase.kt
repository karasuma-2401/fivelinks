package com.karasuma.fivelinks.fivelinks_cmp.database

import com.karasuma.fivelinks.fivelinks_cmp.model.LeaderboardResponse
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerLeaderboardEntry
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.RankTier
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

class LeaderboardDatabase(
    connectionString: String? = null
) {
    private val logger = LoggerFactory.getLogger(LeaderboardDatabase::class.java)

    private val effectiveUrl: String? = connectionString
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("DATABASE_URL")?.takeIf { it.isNotBlank() }
        ?: System.getenv("NEON_DATABASE_URL")?.takeIf { it.isNotBlank() }
        ?: loadDotEnvDatabaseUrl()

    private var dataSource: DataSource? = null
    private val inMemoryFallback = ConcurrentHashMap<String, PlayerLeaderboardEntry>()

    init {
        tryConnect()
    }

    private fun loadDotEnvDatabaseUrl(): String? {
        val candidateFiles = listOf(
            File(".env"),
            File("../.env"),
            File("local.properties"),
            File("../local.properties")
        )
        for (file in candidateFiles) {
            if (!file.isFile) continue
            runCatching {
                file.useLines { lines ->
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                        val key = trimmed.substringBefore('=').trim()
                        if (key == "DATABASE_URL" || key == "NEON_DATABASE_URL") {
                            val value = trimmed.substringAfter('=', "").trim().removeSurrounding("\"").removeSurrounding("'")
                            if (value.isNotEmpty()) return value
                        }
                    }
                }
            }
        }
        return null
    }

    private fun tryConnect() {
        val rawUrl = effectiveUrl
        if (rawUrl.isNullOrBlank()) {
            logger.info("No DATABASE_URL configured; leaderboard running in in-memory mode.")
            return
        }
        try {
            val parsed = parsePostgresUrl(rawUrl)

            val config = HikariConfig().apply {
                this.jdbcUrl = parsed.jdbcUrl
                if (parsed.username != null) this.username = parsed.username
                if (parsed.password != null) this.password = parsed.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                connectionTimeout = 8000
                idleTimeout = 30000
                maxLifetime = 60000
                poolName = "FiveLinksNeonPool"
            }
            val ds = HikariDataSource(config)
            initSchema(ds)
            dataSource = ds
            logger.info("Successfully connected to Neon PostgreSQL database.")
        } catch (e: Exception) {
            logger.warn("Failed to connect to PostgreSQL (${e.message}). Falling back to in-memory store.")
            dataSource = null
        }
    }

    private data class ParsedJdbcConfig(
        val jdbcUrl: String,
        val username: String?,
        val password: String?,
    )

    private fun parsePostgresUrl(rawUrl: String): ParsedJdbcConfig {
        val trimmed = rawUrl.trim()
        val normalized = when {
            trimmed.startsWith("jdbc:postgresql://") -> trimmed.removePrefix("jdbc:")
            trimmed.startsWith("postgres://") -> "postgresql://" + trimmed.removePrefix("postgres://")
            else -> trimmed
        }
        if (!normalized.startsWith("postgresql://")) {
            return ParsedJdbcConfig(jdbcUrl = trimmed, username = null, password = null)
        }

        val uri = URI(normalized)
        val userInfo = uri.rawUserInfo
        val username = userInfo?.substringBefore(':')?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val password = userInfo?.substringAfter(':', "")?.takeIf { it.isNotEmpty() }
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }

        val host = uri.host ?: "localhost"
        val port = if (uri.port > 0) uri.port else 5432
        val path = uri.path?.takeIf { it.isNotEmpty() } ?: "/neondb"

        // Filter out libpq-only parameters like channel_binding that pgjdbc does not use
        val supportedParams = uri.rawQuery
            ?.split("&")
            ?.filter { param ->
                val key = param.substringBefore('=').lowercase()
                key != "channel_binding" && key.isNotBlank()
            }
            ?.joinToString("&")
            .orEmpty()

        val queryPart = if (supportedParams.isNotEmpty()) "?$supportedParams" else "?sslmode=require"
        return ParsedJdbcConfig(
            jdbcUrl = "jdbc:postgresql://$host:$port$path$queryPart",
            username = username,
            password = password
        )
    }

    private fun initSchema(ds: DataSource) {
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS leaderboard (
                        player_id VARCHAR(64) PRIMARY KEY,
                        player_name VARCHAR(64) NOT NULL,
                        rating INT NOT NULL DEFAULT 1000,
                        tier VARCHAR(32) NOT NULL DEFAULT 'BRONZE',
                        matches_played INT NOT NULL DEFAULT 0,
                        wins INT NOT NULL DEFAULT 0,
                        sequences_completed INT NOT NULL DEFAULT 0,
                        snipes_count INT NOT NULL DEFAULT 0,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
                    );
                    CREATE INDEX IF NOT EXISTS idx_leaderboard_rating ON leaderboard(rating DESC);
                    """.trimIndent()
                )
            }
            seedInitialPlayersIfEmpty(conn)
        }
    }

    private fun seedInitialPlayersIfEmpty(conn: Connection) {
        val count = conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT COUNT(*) FROM leaderboard")
            if (rs.next()) rs.getInt(1) else 0
        }
        if (count > 0) return

        val seeds = listOf(
            PlayerLeaderboardEntry(0, "bot_gm_karasuma", "Karasuma", 2560, RankTier.GRANDMASTER, 84, 66, 142, 49),
            PlayerLeaderboardEntry(0, "bot_dia_linh", "Minh Linh", 2180, RankTier.DIAMOND, 62, 44, 98, 31),
            PlayerLeaderboardEntry(0, "bot_plat_hoang", "Hoàng Long", 1890, RankTier.PLATINUM, 50, 33, 76, 24),
            PlayerLeaderboardEntry(0, "bot_gold_vy", "Khánh Vy", 1580, RankTier.GOLD, 38, 23, 52, 18),
            PlayerLeaderboardEntry(0, "bot_sil_nam", "Hải Nam", 1290, RankTier.SILVER, 25, 14, 31, 9),
            PlayerLeaderboardEntry(0, "bot_brz_tan", "Tân Binh", 1040, RankTier.BRONZE, 12, 6, 14, 4),
        )
        val sql = """
            INSERT INTO leaderboard (player_id, player_name, rating, tier, matches_played, wins, sequences_completed, snipes_count, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (player_id) DO NOTHING
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            for (s in seeds) {
                stmt.setString(1, s.playerId)
                stmt.setString(2, s.playerName)
                stmt.setInt(3, s.rating)
                stmt.setString(4, s.tier.name)
                stmt.setInt(5, s.matchesPlayed)
                stmt.setInt(6, s.wins)
                stmt.setInt(7, s.sequencesCompleted)
                stmt.setInt(8, s.snipesCount)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }

    fun getTopPlayers(limit: Int = 50, currentPlayerId: String? = null): LeaderboardResponse {
        val safeLimit = limit.coerceIn(1, 100)
        val safePlayerId = currentPlayerId?.let(::sanitizePlayerId)?.takeIf { it.isNotEmpty() }
        val ds = dataSource
        if (ds != null) {
            try {
                return ds.connection.use { conn ->
                    fetchFromPostgres(conn, safeLimit, safePlayerId)
                }
            } catch (e: Exception) {
                logger.warn("Postgres query failed, serving from fallback: ${e.message}")
            }
        }
        return fetchFromMemory(safeLimit, safePlayerId)
    }

    fun syncPlayer(req: PlayerSyncRequest, isServerAuthoritative: Boolean = false): PlayerLeaderboardEntry {
        val safeId = sanitizePlayerId(req.playerId)
        require(safeId.isNotEmpty()) { "Invalid playerId" }
        val safeName = sanitizePlayerName(req.playerName)
        val maxGain = if (isServerAuthoritative) 30 else 25
        val minLoss = if (isServerAuthoritative) -20 else -15
        val safeReq = req.copy(
            playerId = safeId,
            playerName = safeName,
            ratingDelta = req.ratingDelta.coerceIn(minLoss, maxGain),
            sequencesCount = req.sequencesCount.coerceIn(0, 2),
            snipesCount = req.snipesCount.coerceIn(0, 8),
        )
        val ds = dataSource
        if (ds != null) {
            try {
                return ds.connection.use { conn ->
                    upsertInPostgres(conn, safeReq)
                }
            } catch (e: Exception) {
                logger.warn("Postgres sync failed, upserting in fallback: ${e.message}")
            }
        }
        return upsertInMemory(safeReq)
    }

    private fun sanitizePlayerId(raw: String): String =
        raw.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(64)

    private fun sanitizePlayerName(raw: String): String =
        raw.trim()
            .replace(Regex("[\\p{Cntrl}<>\"'`]"), "")
            .take(24)
            .ifBlank { "Người chơi" }

    private fun fetchFromPostgres(conn: Connection, limit: Int, currentPlayerId: String?): LeaderboardResponse {
        val topList = mutableListOf<PlayerLeaderboardEntry>()
        val selectSql = "SELECT player_id, player_name, rating, tier, matches_played, wins, sequences_completed, snipes_count FROM leaderboard ORDER BY rating DESC, wins DESC LIMIT ?"
        conn.prepareStatement(selectSql).use { stmt ->
            stmt.setInt(1, limit)
            val rs = stmt.executeQuery()
            var rank = 1
            while (rs.next()) {
                val tierStr = rs.getString("tier") ?: "BRONZE"
                val tier = try { RankTier.valueOf(tierStr) } catch (_: Exception) { RankTier.BRONZE }
                topList.add(
                    PlayerLeaderboardEntry(
                        rank = rank++,
                        playerId = rs.getString("player_id"),
                        playerName = rs.getString("player_name"),
                        rating = rs.getInt("rating"),
                        tier = tier,
                        matchesPlayed = rs.getInt("matches_played"),
                        wins = rs.getInt("wins"),
                        sequencesCompleted = rs.getInt("sequences_completed"),
                        snipesCount = rs.getInt("snipes_count")
                    )
                )
            }
        }

        var current: PlayerLeaderboardEntry? = null
        if (currentPlayerId != null) {
            current = topList.firstOrNull { it.playerId == currentPlayerId }
            if (current == null) {
                // Query player rank
                val currentSql = """
                    WITH ranked AS (
                        SELECT player_id, player_name, rating, tier, matches_played, wins, sequences_completed, snipes_count,
                               RANK() OVER (ORDER BY rating DESC, wins DESC) as pos
                        FROM leaderboard
                    )
                    SELECT * FROM ranked WHERE player_id = ?
                """.trimIndent()
                conn.prepareStatement(currentSql).use { stmt ->
                    stmt.setString(1, currentPlayerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val tierStr = rs.getString("tier") ?: "BRONZE"
                        val tier = try { RankTier.valueOf(tierStr) } catch (_: Exception) { RankTier.BRONZE }
                        current = PlayerLeaderboardEntry(
                            rank = rs.getInt("pos"),
                            playerId = rs.getString("player_id"),
                            playerName = rs.getString("player_name"),
                            rating = rs.getInt("rating"),
                            tier = tier,
                            matchesPlayed = rs.getInt("matches_played"),
                            wins = rs.getInt("wins"),
                            sequencesCompleted = rs.getInt("sequences_completed"),
                            snipesCount = rs.getInt("snipes_count")
                        )
                    }
                }
            }
        }

        var total = 0
        conn.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT COUNT(*) FROM leaderboard")
            if (rs.next()) total = rs.getInt(1)
        }

        return LeaderboardResponse(
            topPlayers = topList,
            currentPlayer = current,
            totalPlayers = total.coerceAtLeast(topList.size)
        )
    }

    private fun upsertInPostgres(conn: Connection, req: PlayerSyncRequest): PlayerLeaderboardEntry {
        val upsertSql = """
            INSERT INTO leaderboard (player_id, player_name, rating, tier, matches_played, wins, sequences_completed, snipes_count, updated_at)
            VALUES (?, ?, GREATEST(0, 1000 + ?), ?, 1, ?, ?, ?, NOW())
            ON CONFLICT (player_id) DO UPDATE SET
                player_name = EXCLUDED.player_name,
                rating = GREATEST(0, leaderboard.rating + ?),
                tier = CASE
                    WHEN GREATEST(0, leaderboard.rating + ?) >= 2500 THEN 'GRANDMASTER'
                    WHEN GREATEST(0, leaderboard.rating + ?) >= 2100 THEN 'DIAMOND'
                    WHEN GREATEST(0, leaderboard.rating + ?) >= 1800 THEN 'PLATINUM'
                    WHEN GREATEST(0, leaderboard.rating + ?) >= 1500 THEN 'GOLD'
                    WHEN GREATEST(0, leaderboard.rating + ?) >= 1200 THEN 'SILVER'
                    ELSE 'BRONZE'
                END,
                matches_played = leaderboard.matches_played + 1,
                wins = leaderboard.wins + ?,
                sequences_completed = leaderboard.sequences_completed + ?,
                snipes_count = leaderboard.snipes_count + ?,
                updated_at = NOW()
            RETURNING player_id, player_name, rating, tier, matches_played, wins, sequences_completed, snipes_count
        """.trimIndent()

        val isWinInt = if (req.isWin) 1 else 0
        return conn.prepareStatement(upsertSql).use { stmt ->
            val initialTier = RankTier.fromRating(1000 + req.ratingDelta).name
            stmt.setString(1, req.playerId)
            stmt.setString(2, req.playerName)
            stmt.setInt(3, req.ratingDelta)
            stmt.setString(4, initialTier)
            stmt.setInt(5, isWinInt)
            stmt.setInt(6, req.sequencesCount)
            stmt.setInt(7, req.snipesCount)

            // Update part
            stmt.setInt(8, req.ratingDelta)
            stmt.setInt(9, req.ratingDelta)
            stmt.setInt(10, req.ratingDelta)
            stmt.setInt(11, req.ratingDelta)
            stmt.setInt(12, req.ratingDelta)
            stmt.setInt(13, req.ratingDelta)
            stmt.setInt(14, isWinInt)
            stmt.setInt(15, req.sequencesCount)
            stmt.setInt(16, req.snipesCount)

            val rs = stmt.executeQuery()
            if (rs.next()) {
                val rating = rs.getInt("rating")
                val dynamicTier = RankTier.fromRating(rating)
                PlayerLeaderboardEntry(
                    rank = 0,
                    playerId = rs.getString("player_id"),
                    playerName = rs.getString("player_name"),
                    rating = rating,
                    tier = dynamicTier,
                    matchesPlayed = rs.getInt("matches_played"),
                    wins = rs.getInt("wins"),
                    sequencesCompleted = rs.getInt("sequences_completed"),
                    snipesCount = rs.getInt("snipes_count")
                )
            } else {
                upsertInMemory(req)
            }
        }
    }

    private fun fetchFromMemory(limit: Int, currentPlayerId: String?): LeaderboardResponse {
        val sorted = inMemoryFallback.values.sortedWith(
            compareByDescending<PlayerLeaderboardEntry> { it.rating }.thenByDescending { it.wins }
        )
        val topList = sorted.take(limit).mapIndexed { idx, p -> p.copy(rank = idx + 1) }
        val current = if (currentPlayerId != null) {
            val foundIdx = sorted.indexOfFirst { it.playerId == currentPlayerId }
            if (foundIdx >= 0) sorted[foundIdx].copy(rank = foundIdx + 1) else null
        } else null

        return LeaderboardResponse(
            topPlayers = topList,
            currentPlayer = current,
            totalPlayers = sorted.size
        )
    }

    private fun upsertInMemory(req: PlayerSyncRequest): PlayerLeaderboardEntry {
        val existing = inMemoryFallback[req.playerId]
        val newRating = (existing?.rating ?: 1000) + req.ratingDelta
        val newTier = RankTier.fromRating(newRating)
        val updated = PlayerLeaderboardEntry(
            rank = 0,
            playerId = req.playerId,
            playerName = req.playerName,
            rating = newRating.coerceAtLeast(0),
            tier = newTier,
            matchesPlayed = (existing?.matchesPlayed ?: 0) + 1,
            wins = (existing?.wins ?: 0) + (if (req.isWin) 1 else 0),
            sequencesCompleted = (existing?.sequencesCompleted ?: 0) + req.sequencesCount,
            snipesCount = (existing?.snipesCount ?: 0) + req.snipesCount
        )
        inMemoryFallback[req.playerId] = updated
        return updated
    }
}

