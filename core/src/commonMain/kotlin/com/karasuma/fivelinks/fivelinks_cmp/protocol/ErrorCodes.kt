package com.karasuma.fivelinks.fivelinks_cmp.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class ErrorCodes {
    PROTOCOL_VERSION_MISMATCH,
    INTERNAL_ERROR
}