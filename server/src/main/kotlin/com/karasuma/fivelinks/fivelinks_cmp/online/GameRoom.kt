package com.karasuma.fivelinks.fivelinks_cmp.online

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.protocol.GameResult
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomStatus
import kotlinx.coroutines.Job
import kotlin.time.ComparableTimeMark

/** A player known to the server, connected or not; [token] lets them resume after a drop. */
internal class PlayerSession(
    val playerId: PlayerId,
    val token: String,
    val name: String,
    /** When the session was opened or last lost its socket. */
    var lastSeen: ComparableTimeMark,
) {
    var connection: ClientConnection? = null
    var room: GameRoom? = null
}

/** A player's place in a room. */
internal class Seat(val session: PlayerSession, val team: Team) {
    var wantsRematch = false
    /** While the player is away mid-game: when they forfeit. */
    var forfeitAt: ComparableTimeMark? = null
    var forfeitJob: Job? = null

    val connected: Boolean get() = session.connection != null
}

internal class GameRoom(val code: String, val settings: RoomSettings) {
    /** The creator first. */
    val seats = mutableListOf<Seat>()
    var status = RoomStatus.WAITING
    var game: GameState? = null
    /** Counts the games played here, so a timer left from one game never touches the next. */
    var gameNumber = 0
    /** Who moved first in the current game; a rematch hands the first move to the other player. */
    var firstPlayerId: PlayerId? = null
    var result: GameResult? = null
    var turnEndsAt: ComparableTimeMark? = null
    var turnJob: Job? = null
    var expiryJob: Job? = null

    fun seatOf(session: PlayerSession): Seat = seats.first { it.session === session }
}
