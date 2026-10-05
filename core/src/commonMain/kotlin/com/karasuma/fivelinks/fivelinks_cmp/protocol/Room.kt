package com.karasuma.fivelinks.fivelinks_cmp.protocol

import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlinx.serialization.Serializable

/** Rules the room creator picks; both players play by them. */
@Serializable
data class RoomSettings(
    val sequenceToWin: Int = 2,
    val tacticalCrafting: Boolean = true,
)

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
