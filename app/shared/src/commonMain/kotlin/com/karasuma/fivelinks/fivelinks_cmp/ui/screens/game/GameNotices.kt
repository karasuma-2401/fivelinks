package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.GameNotice
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIcon
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.displayName
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

/**
 * Describes what changed between two consecutive states, or null when there is
 * nothing worth announcing (a new game, the player's own move against the AI,
 * or the final move, which the end screen covers).
 */
internal fun moveNotice(previous: GameState, current: GameState, id: Long): GameNotice? {
    if (current.turnNumber <= previous.turnNumber || current.isGameOver) return null
    val move = current.lastMove ?: return null
    val mover = current.players.firstOrNull { it.id == move.playerId } ?: return null
    val soloVsAi = current.players.count { !it.isAi } == 1

    // A completed sequence matters more than the move that made it.
    val newSequence = current.completedSequence.firstOrNull { it !in previous.completedSequence }
    if (newSequence != null) {
        val team = newSequence.team
        return GameNotice(
            id = id,
            text = "${actorName(team, current, soloVsAi)} hoàn thành 1 hàng! " +
                "(${current.sequencesOf(team)}/${current.config.sequenceToWin})",
            accent = team.primaryColor(),
            icon = LineIcon.Star
        )
    }

    // Against the AI the player already knows what they just did, except for a
    // Thiên Phạt, which deserves the fanfare.
    if (soloVsAi && !mover.isAi && move !is Move.DivineWipe) return null

    val actor = actorName(mover.team, current, soloVsAi)
    val teamColor = mover.team.primaryColor()
    return when (move) {
        is Move.Place -> if (move.card.isJack()) {
            GameNotice(id, "$actor dùng J 2 mắt, đặt quân vào", teamColor, LineIcon.Sparkle, current.board.cardAt(move.position))
        } else {
            GameNotice(id, "$actor đặt quân vào", teamColor, LineIcon.Chip, current.board.cardAt(move.position))
        }
        is Move.CraftPlace ->
            GameNotice(id, "$actor ghép đôi, đặt quân vào", teamColor, LineIcon.Sparkle, current.board.cardAt(move.position))
        is Move.Remove, is Move.CraftRemove -> {
            val position = if (move is Move.Remove) move.position else (move as Move.CraftRemove).position
            val target = if (soloVsAi) "bắn tỉa quân của bạn tại" else "bắn tỉa quân tại"
            GameNotice(id, "$actor $target", BrandRed, LineIcon.Crosshair, current.board.cardAt(position))
        }
        is Move.DivineWipe -> {
            val text = when {
                !soloVsAi -> "$actor kích hoạt Thiên Phạt Hoàng Kim!"
                mover.isAi -> "AI kích hoạt Thiên Phạt! Quân của bạn bị xóa sạch"
                else -> "Bạn kích hoạt Thiên Phạt! Quân đối thủ bị xóa sạch"
            }
            GameNotice(id, text, GoldAccent, LineIcon.Bolt, emphasis = true)
        }
        is Move.SwapDeadCard -> GameNotice(id, "$actor đổi một lá bài chết", teamColor, LineIcon.Restart)
    }
}

private fun actorName(team: Team, state: GameState, soloVsAi: Boolean): String {
    val members = state.players.filter { it.team == team }
    return when {
        members.any { it.isAi } -> "AI"
        soloVsAi -> "Bạn"
        else -> "Đội ${team.displayName()}"
    }
}
