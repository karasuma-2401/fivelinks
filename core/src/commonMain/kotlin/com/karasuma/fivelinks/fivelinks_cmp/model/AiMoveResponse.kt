package com.karasuma.fivelinks.fivelinks_cmp.model

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class AiMoveResponse(
    val move: Move
)
