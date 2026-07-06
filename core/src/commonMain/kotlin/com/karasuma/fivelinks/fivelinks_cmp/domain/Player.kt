package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

// make code so easy to understand
typealias PlayerId = String

@Serializable
data class Player(
    val id: PlayerId,
    val name: String,
    val team: Team,
    val isAi: Boolean = false
)
