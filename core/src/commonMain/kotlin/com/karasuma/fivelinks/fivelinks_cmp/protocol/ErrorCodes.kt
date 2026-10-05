package com.karasuma.fivelinks.fivelinks_cmp.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorCodes {
    PROTOCOL_VERSION_MISMATCH,
    INTERNAL_ERROR,
    INVALID_REQUEST,
    ROOM_NOT_FOUND,
    ROOM_FULL,
    /** The request needs a room the player is not in. */
    NOT_IN_ROOM,
    NOT_YOUR_TURN,
    /** The game engine refused the move; the message says why. */
    ILLEGAL_MOVE,
    /** The move was made for a turn that has already passed. */
    STALE_TURN,
    /** The session token is unknown or has expired; start a new session. */
    SESSION_EXPIRED,
}