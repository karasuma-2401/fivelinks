package com.karasuma.fivelinks.fivelinks_cmp.database

import com.karasuma.fivelinks.fivelinks_cmp.model.LeaderboardResponse
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerLeaderboardEntry
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncRequest
import com.karasuma.fivelinks.fivelinks_cmp.model.RankTier
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

class LeaderboardDatabase(
    connectionString: String? = null
) {
    private val logger = LoggerFactory.getLogger(LeaderboardDatabase::class.java)

    private val defaultNeonUrl = "postgresql://neondb_owner:npg_vwKUj6q8PzWF@ep-bold-art-ao7qwhdb.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
    private val effectiveUrl = connectionString
        ?: System.getenv("DATABASE_URL")
        ?: System.getenv("NEON_DATABASE_URL")
        ?: defaultNeonUrl

    private var dataSource: DataSource? = null
    private val inMemoryFallback = ConcurrentHashMap<String, PlayerLeaderboardEntry>()

    init {
        tryConnect()
    }

    private fun tryConnect() {
        try {
            // Convert standard postgres URL to JDBC format if needed
            val jdbcUrl = if (effectiveUrl.startsWith("postgres://") || effectiveUrl.startsWith("postgresql://")) {
                "jdbc:" + effectiveUrl
            } else {
                effectiveUrl
            }

            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                connectionTimeout = 5000
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
        }
    }

    fun getTopPlayers(limit: Int = 50, currentPlayerId: String? = null): LeaderboardResponse {
        val ds = dataSource
        if (ds != null) {
            try {
                return ds.connection.use { conn ->
                    fetchFromPostgres(conn, limit, currentPlayerId)
                }
            } catch (e: Exception) {
                logger.warn("Postgres query failed, serving from fallback: ${e.message}")
            }
        }
        return fetchFromMemory(limit, currentPlayerId)
    }

    fun syncPlayer(req: PlayerSyncRequest): PlayerLeaderboardEntry {
        val ds = dataSource
        if (ds != null) {
            try {
                return ds.connection.use { conn ->
                    upsertInPostgres(conn, req)
                }
            } catch (e: Exception) {
                logger.warn("Postgres sync failed, upserting in fallback: ${e.message}")
            }
        }
        return upsertInMemory(req)
    }

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
                tier = ?,
                matches_played = leaderboard.matches_played + 1,
                wins = leaderboard.wins + ?,
                sequences_completed = leaderboard.sequences_completed + ?,
                snipes_count = leaderboard.snipes_count + ?,
                updated_at = NOW()
            RETURNING player_id, player_name, rating, tier, matches_played, wins, sequences_completed, snipes_count
        """.trimIndent()

        // Calculate expected new tier based on rating
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
            val updatedTier = RankTier.fromRating(1000 + req.ratingDelta).name
            stmt.setString(9, updatedTier)
            stmt.setInt(10, isWinInt)
            stmt.setInt(11, req.sequencesCount)
            stmt.setInt(12, req.snipesCount)

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

