package com.karasuma.fivelinks.fivelinks_cmp.model

import kotlinx.serialization.Serializable

@Serializable
enum class RankTier(val displayName: String, val minRating: Int, val badgeColorHex: Long) {
    BRONZE("Đồng", 0, 0xFFCD7F32),
    SILVER("Bạc", 1200, 0xFFC0C0C0),
    GOLD("Vàng", 1500, 0xFFFFD700),
    PLATINUM("Bạch Kim", 1800, 0xFFE5E4E2),
    DIAMOND("Kim Cương", 2100, 0xFFB9F2FF),
    GRANDMASTER("Đại Kiện Tướng", 2500, 0xFFFF4500);

    companion object {
        fun fromRating(rating: Int): RankTier = when {
            rating >= 2500 -> GRANDMASTER
            rating >= 2100 -> DIAMOND
            rating >= 1800 -> PLATINUM
            rating >= 1500 -> GOLD
            rating >= 1200 -> SILVER
            else -> BRONZE
        }
    }
}

@Serializable
data class PlayerLeaderboardEntry(
    val rank: Int = 0,
    val playerId: String,
    val playerName: String,
    val rating: Int = 1000,
    val tier: RankTier = RankTier.BRONZE,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val sequencesCompleted: Int = 0,
    val snipesCount: Int = 0
) {
    val winRatePercent: Int
        get() = if (matchesPlayed > 0) ((wins.toFloat() / matchesPlayed) * 100).toInt() else 0
}

@Serializable
data class LeaderboardResponse(
    val topPlayers: List<PlayerLeaderboardEntry>,
    val currentPlayer: PlayerLeaderboardEntry? = null,
    val totalPlayers: Int = 0
)

@Serializable
data class PlayerSyncRequest(
    val playerId: String,
    val playerName: String,
    val ratingDelta: Int = 0,
    val isWin: Boolean = false,
    val sequencesCount: Int = 0,
    val snipesCount: Int = 0
)

@Serializable
data class PlayerSyncResponse(
    val success: Boolean,
    val updatedEntry: PlayerLeaderboardEntry
)

