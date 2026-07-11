package com.karasuma.fivelinks.fivelinks_cmp.ai

data class HeuristicWeights (
    val completeSequence: Double = 10_000.0,
    val selfOpenFour: Double = 2_000.0,
    val blockOpponentOpenFour: Double = 1_800.0,
    val extendSelfPerChip: Double = 100.0,
    val blockOpponentPerChip: Double = 80.0,
    val centerControl: Double = 15.0,
    val cornerAdjacency: Double = 40.0,
    val wasteWildJack: Double = -200.0,
    val wasteRemoveJack: Double = -150.0,
    val doubleThreat: Double = 250.0,
    val defensiveMultiplier: Double = 0.8,
    val swapDeadCard: Double = 50.0,
) {
    companion object {
        val default: HeuristicWeights = HeuristicWeights()
    }
}