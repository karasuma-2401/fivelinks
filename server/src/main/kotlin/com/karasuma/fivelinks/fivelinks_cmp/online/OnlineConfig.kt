package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Timing rules for online rooms. */
data class OnlineConfig(
    /** A player who does not move in time passes. */
    val turnTime: Duration = 60.seconds,
    /** A player who stays disconnected this long during a game forfeits it. */
    val reconnectGrace: Duration = 60.seconds,
    /** A room nobody joins is closed after this long. */
    val waitingRoomTtl: Duration = 10.minutes,
    /** A session with no socket and no room is forgotten after this long. */
    val idleSessionTtl: Duration = 30.minutes,
)
