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
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue

@Composable
fun RulesDialog(
    onDismiss: () -> Unit
) {
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
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .heightIn(max = 660.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HƯỚNG DẪN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                color = BrandRed
                            )
                            Text(
                                text = "Luật chơi FiveLink",
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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

                    // Nội dung luật cuộn mượt mà
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        RuleSection(
                            number = "01",
                            title = "Luật cốt lõi",
                            subtitle = "QUY TẮC BẢNG ĐẤU CƠ BẢN"
                        ) {
                            RuleItem(
                                text = "Bàn cờ 10×10 gồm 100 ô tương ứng bộ bài Tây chuẩn (không có 4 lá J trên bàn)."
                            )
                            RuleItem(
                                text = "4 ô góc có biểu tượng ngôi sao (★) là ô tự do, tính sẵn chuỗi cho cả hai người chơi."
                            )
                            RuleItem(
                                text = "Mục tiêu: Đặt quân tạo thành hàng 5 quân liên tiếp (ngang, dọc hoặc chéo)."
                            )
                            RuleItem(
                                tag = "TỰ DO",
                                tagIcon = LineIcon.Sparkle,
                                tagColor = TeamBlue,
                                title = "J 2 mắt (J Rô, J Chuồn)",
                                text = "Quân bài toàn năng: Cho phép đặt 1 quân cờ vào bất kỳ ô trống nào trên bàn."
                            )
                            RuleItem(
                                tag = "BẮN TỈA",
                                tagIcon = LineIcon.Crosshair,
                                tagColor = BrandRed,
                                title = "J 1 mắt (J Cơ, J Bích)",
                                text = "Xạ thủ bắn tỉa: Gỡ bỏ 1 quân cờ đối thủ (trừ các quân đã khóa trong chuỗi 5)."
                            )
                            RuleItem(
                                text = "Lá bài chết: Khi cả 2 ô trên bàn cờ của lá bài đều đã bị chiếm, bạn được chọn lá đó để rút lá bài mới thay thế."
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        RuleSection(
                            number = "02",
                            title = "Cơ chế chiến thuật",
                            subtitle = "KÍCH HOẠT KHI BẬT TACTICAL MODE"
                        ) {
                            RuleItem(
                                tag = "GHÉP ĐÔI",
                                tagIcon = LineIcon.Sparkle,
                                tagColor = TeamBlue,
                                title = "Ghép 2 lá cùng số",
                                text = "Tạo Jack 2 Mắt nhân tạo: Bỏ 2 lá để đặt 1 quân vào ô trống bất kỳ và lập tức rút 2 lá mới."
                            )
                            RuleItem(
                                tag = "ĐỒNG CHẤT",
                                tagIcon = LineIcon.Crosshair,
                                tagColor = BrandRed,
                                title = "Ghép 2 lá liên tiếp cùng chất",
                                text = "Tạo Jack 1 Mắt nhân tạo: Bỏ 2 lá để bắn tỉa 1 quân đối thủ chưa khóa và lập tức rút 2 lá mới."
                            )
                            RuleItem(
                                tag = "THIÊN PHẠT",
                                tagIcon = LineIcon.Bolt,
                                tagColor = GoldAccent,
                                title = "Thiên Phạt Hoàng Kim",
                                text = "Chiêu thức tối thượng huyền thoại: Quét sạch toàn bộ quân cờ và tất cả chuỗi đã khóa của đối phương trên bàn đấu!"
                            )
                            RuleItem(
                                tag = "KHÓA CHUỖI",
                                tagIcon = LineIcon.Chip,
                                tagColor = Color(0xFF10B981),
                                title = "Bảo vệ chuỗi vĩnh viễn",
                                text = "Khi hàng 5 quân được hoàn thành, toàn bộ chuỗi được khóa bất tử, đối thủ không thể dùng J 1 Mắt để gỡ bỏ."
                            )
                        }
                    }

                    // Nút Đã hiểu
                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp)
                            .height(48.dp)
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
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                color = BrandRed
            )
            Spacer(modifier = Modifier.width(10.dp))
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
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
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
    val hasBadge = tag != null || tagIcon != null
    Row(verticalAlignment = Alignment.Top) {
        if (!hasBadge) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (tag != null) {
                Row(
                    modifier = Modifier.padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tagIcon != null) {
                        LineIconView(
                            icon = tagIcon,
                            color = tagColor,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.4f
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        color = tagColor
                    )
                }
            }
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp)
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
