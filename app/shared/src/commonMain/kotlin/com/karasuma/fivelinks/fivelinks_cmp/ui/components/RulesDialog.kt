package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue

@Composable
fun RulesDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 22.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HƯỚNG DẪN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = BrandRed
                    )
                    Text(
                        text = "Luật chơi",
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .semantics { contentDescription = "Đóng" },
                    contentAlignment = Alignment.Center
                ) {
                    LineIconView(
                        icon = LineIcon.Close,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                RuleSection(number = "01", title = "Luật cốt lõi", subtitle = "ÁP DỤNG CHO MỌI VÁN") {
                    RuleItem(text = "Bàn cờ 10×10 gồm các lá bài tương ứng bộ bài Tây (không có lá J).")
                    RuleItem(text = "4 ô góc có ngôi sao là ô tự do, tính cho tất cả người chơi.")
                    RuleItem(text = "Mục tiêu: xếp đủ số hàng 5 quân liên tiếp (ngang, dọc, chéo) để chiến thắng.")
                    RuleItem(
                        tag = "TỰ DO",
                        tagIcon = LineIcon.Sparkle,
                        tagColor = TeamBlue,
                        text = "J 2 mắt (J rô, J chuồn): đặt quân vào bất kỳ ô trống nào."
                    )
                    RuleItem(
                        tag = "BẮN TỈA",
                        tagIcon = LineIcon.Crosshair,
                        tagColor = BrandRed,
                        text = "J 1 mắt (J cơ, J bích): gỡ 1 quân của đối thủ (trừ quân đã khóa trong hàng 5)."
                    )
                    RuleItem(text = "Bài chết: khi cả 2 ô của lá bài đều đã có quân, chọn lá đó để đổi lá mới.")
                }

                Spacer(modifier = Modifier.height(14.dp))

                RuleSection(number = "02", title = "Cơ chế chiến thuật", subtitle = "KHI BẬT CHẾ ĐỘ CHIẾN THUẬT") {
                    RuleItem(
                        tag = "TỰ DO",
                        tagIcon = LineIcon.Sparkle,
                        tagColor = TeamBlue,
                        title = "Ghép đôi (2 lá cùng số)",
                        text = "Tạo J 2 mắt nhân tạo: đặt 1 quân vào bất kỳ ô trống nào (bỏ 2 lá, rút 2 lá mới)."
                    )
                    RuleItem(
                        tag = "BẮN TỈA",
                        tagIcon = LineIcon.Crosshair,
                        tagColor = BrandRed,
                        title = "Đồng chất liền kề (2 lá liên tiếp cùng chất)",
                        text = "Tạo J 1 mắt nhân tạo: bắn tỉa 1 quân đối thủ chưa khóa (bỏ 2 lá, rút 2 lá mới)."
                    )
                    RuleItem(
                        tag = "THIÊN PHẠT",
                        tagIcon = LineIcon.Bolt,
                        tagColor = GoldAccent,
                        title = "Thiên Phạt Hoàng Kim (sảnh đồng chất 5 lá)",
                        text = "Chiêu thức huyền thoại: xóa sạch toàn bộ quân cờ và hàng khóa của đối phương trên bàn cờ!"
                    )
                }
            }

            Button(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 22.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = "ĐÃ HIỂU",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun RuleSection(
    number: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                color = BrandRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun RuleItem(
    text: String,
    title: String? = null,
    tag: String? = null,
    tagIcon: LineIcon? = null,
    tagColor: Color = BrandRed
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(tagColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            if (tag != null) {
                Row(
                    modifier = Modifier.padding(bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tagIcon != null) {
                        LineIconView(icon = tagIcon, color = tagColor, modifier = Modifier.size(12.dp), strokeWidth = 2.6f)
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = tagColor
                    )
                }
            }
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            )
        }
    }
}
