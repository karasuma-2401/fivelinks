package com.karasuma.fivelinks.fivelinks_cmp.model

import com.karasuma.fivelinks.fivelinks_cmp.protocol.ErrorCodes
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: ErrorCodes,
    val message: String,
    val details: String? = null
)
