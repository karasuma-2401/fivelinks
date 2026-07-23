package com.karasuma.fivelinks.fivelinks_cmp.domain

import kotlinx.serialization.Serializable

@Serializable
data class ChipSequence(val team: Team, val positions: List<BoardPosition>)
