package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TextPrimary
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TextSecondary
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
                .shadow(24.dp, shape)
                .clip(shape)
                .background(SurfaceDark)
                .border(1.dp, GoldAccent.copy(alpha = 0.5f), shape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (winner != null) "KẾT THÚC TRẬN ĐẤU" else "KẾT QUẢ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (winner != null) "CHIẾN THẮNG" else "HÒA TRẬN",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(10.dp))

                val teamColor = winner?.primaryColor() ?: Color.White
                val teamName = when (winner) {
                    Team.BLUE -> "Team Xanh (Blue)"
                    Team.RED -> "Team Đỏ (Red)"
                    Team.GREEN -> "Team Xanh Lá (Green)"
                    null -> "Hòa trận"
                }

                Text(
                    text = winningPlayer?.name ?: teamName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = teamColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Số Sequence hoàn thành:", fontSize = 12.sp, color = TextSecondary)
                        Text(" hàng", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tổng số lượt đấu:", fontSize = 12.sp, color = TextSecondary)
                        Text(" lượt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMenuClick,
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Menu", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onRematchClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Chơi lại", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
