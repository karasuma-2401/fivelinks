package com.karasuma.fivelinks.fivelinks_cmp.model

import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class AiMoveRequest(
    val gameState: GameState,
    val playerId: PlayerId,
    val difficulty: Difficulty = Difficulty.EASY,
)
