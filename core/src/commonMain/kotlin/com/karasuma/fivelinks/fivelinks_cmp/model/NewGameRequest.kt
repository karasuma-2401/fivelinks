package com.karasuma.fivelinks.fivelinks_cmp.model

import kotlinx.serialization.Serializable

@Serializable
data class NewGameRequest(
    val playerCount: Int,
    val teamCount: Int,
    val seed: Long = 12L
)
