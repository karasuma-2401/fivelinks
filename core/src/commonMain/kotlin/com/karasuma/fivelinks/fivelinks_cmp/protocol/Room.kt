package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.serialization.Serializable

/** Rules the room creator picks; both players play by them. */
@Serializable
data class RoomSettings(
    val sequenceToWin: Int = 2,
    val tacticalCrafting: Boolean = true,
    /** Who starts the first game; rematches then alternate. */
    val firstPlayer: FirstPlayer = FirstPlayer.RANDOM,
)

@Serializable
enum class FirstPlayer {
    /** The room creator. */
    HOST,
    /** The player who joined with the code. */
    GUEST,
    RANDOM,
}

@Serializable
enum class RoomStatus {
    /** Created, waiting for the second player. */
    WAITING,
    PLAYING,
    /** The game is over; both players may ask for a rematch. */
    FINISHED,
}

@Serializable
data class RoomPlayer(
    val playerId: PlayerId,
    val name: String,
    val team: Team,
    val connected: Boolean = true,
    val wantsRematch: Boolean = false,
)

@Serializable
enum class RoomCloseReason {
    /** Nobody joined the waiting room in time. */
    EXPIRED,
    /** The server is shutting down or restarting for maintenance. */
    SERVER_SHUTDOWN,
}
