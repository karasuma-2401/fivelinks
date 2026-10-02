package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceElevated
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

@Composable
fun GameHeader(
    gameState: GameState,
    isAiThinking: Boolean,
    onRestartClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer
    val playerTeam = currentPlayer.team

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(SurfaceDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Current Player Turn Badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(playerTeam.primaryColor())
                    .border(2.dp, if (isAiThinking) Color.White.copy(alpha = pulseAlpha) else Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = currentPlayer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = if (isAiThinking) "Đang suy nghĩ... 🤖" else "Lượt đi #${gameState.turnNumber + 1}",
                    fontSize = 11.sp,
                    color = if (isAiThinking) GoldAccent else Color(0xFF94A3B8)
                )
            }
        }

        // Center: Sequence Target Score (e.g. 🔵 1/2 vs 🔴 0/2)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val teams = gameState.config.teams
            teams.forEachIndexed { index, team ->
                val seqCount = gameState.sequencesOf(team)
                val target = gameState.config.sequenceToWin
                val symbol = when (team) {
                    Team.BLUE -> "🔵"
                    Team.RED -> "🔴"
                    Team.GREEN -> "🟢"
                }
                Text(
                    text = "$symbol $seqCount/$target",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (index < teams.size - 1) {
                    Text(
                        text = "  vs  ",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Right: Menu & Restart Buttons
        Row {
            OutlinedButton(
                onClick = onRestartClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text("🔄", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onMenuClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
            ) {
                Text("☰", fontSize = 14.sp, color = Color.White)
            }
        }
    }
}
