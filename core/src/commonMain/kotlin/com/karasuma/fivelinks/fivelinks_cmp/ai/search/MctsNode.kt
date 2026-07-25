package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import kotlin.math.ln
import kotlin.math.sqrt

class MctsNode(
    val parent: MctsNode? = null,
    val moveFromParent: Move? = null,
) {
    var visitCount: Int = 0
    var totalValue: Double = 0.0 // tổng value từ góc nhìn root team
    val children: MutableMap<MoveKey, MctsNode> = mutableMapOf()
    var prior: Double = 1.0
    var untried: MutableList<Move> = mutableListOf()
    var expanded: Boolean = false

    val q: Double get() = if (visitCount == 0) 0.0 else totalValue / visitCount

    fun puctScore(cPuct: Double, parentVisits: Int): Double {
        val u = cPuct * prior * sqrt(parentVisits.toDouble().coerceAtLeast(1.0)) / (1 + visitCount)
        return q + u
    }
}

fun selectChild(node: MctsNode, cPuct: Double): MctsNode {
    val parentVisits = node.visitCount
    return node.children.values.maxBy { it.puctScore(cPuct, parentVisits) }
}
