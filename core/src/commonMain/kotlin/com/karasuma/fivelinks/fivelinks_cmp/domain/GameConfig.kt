package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val playerCount: Int,
    val teamsByPlayerIndex: List<Team>,
    val handSize: Int = handSizeForPlayerCount(playerCount),
    val sequenceToWin: Int = sequenceToWinForPlayerCount(teamsByPlayerIndex.size),
    val seed: Long = 0
) {
    init {
        require(playerCount > 0) { "PlayerCount must be positive." }
        require(teamsByPlayerIndex.size == playerCount) { "teams by players index must have the same size as player count"}
        require(handSize > 0) { "handSize must be positive." }
        require(sequenceToWin > 0) { "sequenceToWin must be positive." }
    }

    val teams: Set<Team> = teamsByPlayerIndex.toSet()

    companion object {
        fun handSizeForPlayerCount(playerCount: Int) = when (playerCount) {
            2 -> 7
            in 3..4 -> 6
            6 -> 5
            in 8..9 -> 4
            in 10..12 -> 3
            else -> error("unknown playerCount $playerCount")
        }
        fun soloVsAi(seed: Long) = forPlayer(2, 2, seed)
        fun forPlayer(playerCount: Int, teams: Int, seed: Long): GameConfig {
            val teamOrder = listOf(Team.RED, Team.BLUE, Team.GREEN)
            val teamsByPlayerIndex = (0 until playerCount).map { teamOrder[it % teams] }
            return GameConfig(
                playerCount = playerCount,
                teamsByPlayerIndex = teamsByPlayerIndex,
                seed = seed
            )
        }
        fun sequenceToWinForPlayerCount(teams: Int) = if (teams > 2) 1 else 2
        fun defaultTeamsPlayerCount(playerCount: Int) = when (playerCount) {
            3, 9, 12 -> 3
            else -> 2
        }
        fun allowedTeamsCount(playerCount: Int): List<Int> = when (playerCount) {
            6 -> listOf(2, 3)
            else -> listOf(defaultTeamsPlayerCount(playerCount))
        }
    }
}