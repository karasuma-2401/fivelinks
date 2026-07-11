package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty (val topK: Int, val temperature: Double) {
    EASY(5, 0.9),
    MEDIUM(3, 0.35),
    HARD(1, 0.0),
}

interface AiService {
    suspend fun chooseMove(state: GameState, playerId: PlayerId, difficulty: Difficulty): Move
}