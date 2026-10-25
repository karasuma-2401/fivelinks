package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.components.SegmentedSelector
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue

@Composable
fun RulesDialog(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .heightIn(max = 680.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HƯỚNG DẪN CHIẾN ĐẤU",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                color = BrandRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Luật chơi FiveLink",
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(role = Role.Button, onClick = onDismiss)
                                .semantics { contentDescription = "Đóng" },
                            contentAlignment = Alignment.Center
                        ) {
                            LineIconView(
                                icon = LineIcon.Close,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 2-Tab Segmented Selector
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SegmentedSelector(
                            options = listOf("Luật cơ bản", "Cơ chế chiến thuật"),
                            selectedIndex = selectedTab,
                            onSelect = { selectedTab = it }
                        )
                    }

                    // Scrollable Rule Cards
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { tab ->
                            if (tab == 0) {
                                BasicRulesTab()
                            } else {
                                TacticalRulesTab()
                            }
                        }
                    }

                    // Bottom Action
                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "ĐÃ HIỂU LUẬT",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicRulesTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RuleCard(
            badge = "MỤC TIÊU",
            badgeColor = BrandRed,
            title = "Nối 5 quân liên tiếp",
            description = "Xếp đủ 5 quân cờ cùng màu thành hàng ngang, dọc hoặc chéo để tạo 1 Sequence. Đủ số Sequence mục tiêu là thắng trận!"
        )
        RuleCard(
            badge = "CÁCH ĐÁNH",
            badgeColor = TeamBlue,
            title = "Đặt quân theo lá bài",
            description = "Chọn 1 lá bài trên tay, sau đó chạm vào ô bài tương ứng trên bàn cờ 10×10 để đặt quân cờ của bạn."
        )
        RuleCard(
            badge = "Ô TỰ DO",
            badgeColor = GoldAccent,
            title = "4 ô góc có biểu tượng ngôi sao (★)",
            description = "Là các ô thưởng vĩnh viễn, tự động tính là mắt xích hoàn thành chuỗi cho tất cả người chơi."
        )
        RuleCard(
            badge = "JACK 2 MẮT",
            badgeColor = TeamBlue,
            title = "J Rô (J♦) & J Chuồn (J♣)",
            description = "Quân bài quyền năng: Cho phép đặt 1 quân cờ vào bất kỳ ô trống nào trên toàn bàn cờ."
        )
        RuleCard(
            badge = "JACK 1 MẮT",
            badgeColor = BrandRed,
            title = "J Cơ (J♥) & J Bích (J♠)",
            description = "Quân bài bắn tỉa: Cho phép gỡ bỏ 1 quân cờ của đối thủ (trừ quân cờ đã khóa trong chuỗi Sequence)."
        )
        RuleCard(
            badge = "ĐỔI BÀI",
            badgeColor = Color(0xFF8E8E93),
            title = "Đổi lá bài chết",
            description = "Khi cả 2 ô của 1 lá bài trên bàn đều đã có quân chiếm đóng, chạm vào lá bài đó trên tay để rút lá mới."
        )
    }
}

@Composable
private fun TacticalRulesTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RuleCard(
            badge = "GHÉP ĐÔI",
            badgeColor = TeamBlue,
            title = "2 lá bài cùng số",
            description = "Hóa thân thành Jack 2 Mắt nhân tạo: Bỏ 2 lá cùng số để đặt 1 quân vào ô trống bất kỳ và lập tức rút 2 lá mới."
        )
        RuleCard(
            badge = "ĐỒNG CHẤT KỀ NHAU",
            badgeColor = BrandRed,
            title = "2 lá liên tiếp cùng chất",
            description = "Hóa thân thành Jack 1 Mắt nhân tạo: Bỏ 2 lá kề chất để bắn tỉa 1 quân cờ đối thủ chưa khóa và rút 2 lá mới."
        )
        RuleCard(
            badge = "THIÊN PHẠT HOÀNG KIM",
            badgeColor = GoldAccent,
            title = "Sảnh 5 lá đồng chất trên tay",
            description = "Chiêu thức tối thượng huyền thoại: Quét sạch toàn bộ quân cờ và các chuỗi đã khóa của đối phương trên bàn đấu!"
        )
        RuleCard(
            badge = "BẢO VỆ CHUỖI",
            badgeColor = Color(0xFF34C759),
            title = "Khóa chuỗi Sequence vĩnh viễn",
            description = "Khi hàng 5 quân được hoàn thành, toàn bộ 5 quân cờ sẽ được khóa bảo vệ, đối thủ không thể bắn tỉa gỡ bỏ."
        )
    }
}

@Composable
private fun RuleCard(
    badge: String,
    badgeColor: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = badgeColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
