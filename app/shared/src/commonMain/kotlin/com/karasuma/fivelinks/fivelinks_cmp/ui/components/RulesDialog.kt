package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceElevated
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlueGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRedGlow

@Composable
fun RulesDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .shadow(16.dp, shape)
                .clip(shape)
                .background(SurfaceDark)
                .border(1.5.dp, GoldAccent.copy(alpha = 0.8f), shape)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📖 LUẬT CHƠI FIVELINKS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 1: Classic Rules
                RuleCard(
                    title = "1. Luật Cốt Lõi (Classic Sequence)",
                    color = TeamBlueGlow
                ) {
                    Text("• Bàn cờ 10x10 gồm các lá bài tương ứng bộ bài Tây (bỏ Jacks).", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    Text("• 4 góc bàn cờ là Ô Tự Do (Wild Corner) tính cho tất cả người chơi.", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    Text("• Mục tiêu: Xếp đủ 2 hàng 5 quân cờ liên tiếp (ngang, dọc, chéo) để chiến thắng.", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    Text("• Jack 2 Mắt (Two-Eyed): Đặt quân vào bất kỳ ô trống nào.", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    Text("• Jack 1 Mắt (One-Eyed): Loại bỏ 1 quân cờ của đối thủ (trừ quân đã khóa trong hàng 5).", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                    Text("• Bài Chết (Dead Card): Khi cả 2 ô của lá bài đều bị chiếm, có thể chọn và đổi bài rút lá mới.", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Tactical Crafting
                RuleCard(
                    title = "2. Cơ Chế Chiến Thuật Mới (Tactical Mode)",
                    color = GoldAccent
                ) {
                    Text("⚡ Ghép Đôi (Pair - 2 lá cùng số):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TeamBlueGlow)
                    Text("Tạo ra Jack 2 Mắt nhân tạo ➔ Đặt 1 quân vào bất kỳ ô trống nào (bỏ 2 lá, rút 2 lá mới).", fontSize = 11.sp, color = Color(0xFFE2E8F0))

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🎯 Suited Connector (2 lá liên tiếp đồng chất):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TeamRedGlow)
                    Text("Tạo ra Jack 1 Mắt nhân tạo ➔ Bắn tỉa 1 quân cờ của đối thủ chưa khóa (bỏ 2 lá, rút 2 lá mới).", fontSize = 11.sp, color = Color(0xFFE2E8F0))

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("👑 Thiên Phạt Hoàng Kim (Straight Flush 5 lá):", fontSize = 11.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                    Text("Khi sở hữu sảnh 5 lá đồng chất liên tiếp ➔ Kích hoạt chiêu thức Huyền Thoại: Xóa sạch toàn bộ quân cờ và hàng khóa của đối phương trên bàn cờ!", fontSize = 11.sp, color = Color(0xFFFDE68A))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text("Đã Hiểu", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
