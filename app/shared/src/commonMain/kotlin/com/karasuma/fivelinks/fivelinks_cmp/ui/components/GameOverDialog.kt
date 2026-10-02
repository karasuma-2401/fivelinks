package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceElevated
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

@Composable
fun GameOverDialog(
    gameState: GameState,
    onRematchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val winner = gameState.winner
    val winningPlayer = gameState.players.firstOrNull { it.team == winner }

    Dialog(onDismissRequest = {}) {
        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(SurfaceDark)
                .border(2.dp, GoldAccent, shape)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🏆 CHIẾN THẮNG! 🏆",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(10.dp))

                val teamColor = winner?.primaryColor() ?: Color.White
                val teamName = when (winner) {
                    Team.BLUE -> "Team Xanh (Blue)"
                    Team.RED -> "Team Đỏ (Red)"
                    Team.GREEN -> "Team Xanh Lá (Green)"
                    null -> "Hòa"
                }

                Text(
                    text = winningPlayer?.name ?: teamName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = teamColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Số Sequence hoàn thành:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text("${gameState.sequencesOf(winner ?: Team.BLUE)} hàng", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng số lượt đấu:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text("${gameState.turnNumber} lượt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMenuClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Menu", color = Color.White)
                    }

                    Button(
                        onClick = onRematchClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Chơi lại", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
