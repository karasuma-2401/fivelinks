package com.karasuma.fivelinks.fivelinks_cmp.ai

enum class EvalMode {
    HeuristicOnly,
    Hybrid, // NN + heuristic fallback (Phase 7)
    NeuralFirst, // prefer NN when available; still falls back
}
data class DifficultyConfig(
    val maxSimulations: Int,
    val timeBudgetMs: Long,
    val temperature: Double,
    val topK: Int,
    val useTacticalForced: Boolean,
    val determinizations: Int,
    val evalMode: EvalMode,
    val cPuct: Double = 1.5
) {
    companion object {
        fun from(difficulty: Difficulty): DifficultyConfig = when (difficulty) {
            // specs have been done by AI (specially CURSOR, don't ask me about that)
            Difficulty.EASY -> DifficultyConfig(
                maxSimulations = 80,
                timeBudgetMs = 50,
                temperature = 0.9,
                topK = 5,
                useTacticalForced = true,
                determinizations = 2,
                evalMode = EvalMode.HeuristicOnly,
            )
            Difficulty.MEDIUM -> DifficultyConfig(
                maxSimulations = 400,
                timeBudgetMs = 150,
                temperature = 0.35,
                topK = 3,
                useTacticalForced = true,
                determinizations = 4,
                evalMode = EvalMode.HeuristicOnly,
            )
            Difficulty.HARD -> DifficultyConfig(
                maxSimulations = 2000,
                timeBudgetMs = 600,
                temperature = 0.0,
                topK = 1,
                useTacticalForced = true,
                determinizations = 6, // Reduced from 9 to 6 to improve latency
                evalMode = EvalMode.Hybrid,
            )
        }

        /** Self-play data gen: keep visit diversity (temperature > 0), budget lighter than HARD. */
        fun selfPlay(): DifficultyConfig = DifficultyConfig(
            maxSimulations = 300,
            timeBudgetMs = 200,
            temperature = 1.0,
            topK = 15,
            useTacticalForced = true,
            determinizations = 4,
            evalMode = EvalMode.HeuristicOnly,
        )
    }
}