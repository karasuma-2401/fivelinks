package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.TextButton
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
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue

private data class OnboardingStep(
    val stepNumber: String,
    val title: String,
    val subtitle: String,
    val icon: LineIcon,
    val iconBgColor: Color,
    val iconColor: Color,
    val points: List<Pair<String?, String>>,
)

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit = onDismiss,
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                stepNumber = "01 / 04",
                title = "Mục tiêu chiến thắng",
                subtitle = "LUẬT CHƠI CỐT LÕI",
                icon = LineIcon.Sparkle,
                iconBgColor = TeamBlue.copy(alpha = 0.15f),
                iconColor = TeamBlue,
                points = listOf(
                    "XẾP HÀNG 5 QUÂN" to "Xếp 5 quân cùng màu liên tiếp theo hàng ngang, dọc hoặc chéo để tạo thành một chuỗi (Sequence) chiến thắng.",
                    "4 Ô GÓC TỰ DO" to "Bốn ô góc có biểu tượng ngôi sao là ô tự do vĩnh viễn, được tính là quân cờ hợp lệ cho tất cả người chơi.",
                    "ĐIỀU KIỆN THẮNG" to "Đạt đủ số chuỗi quy định của ván đấu (1 hàng cho ván nhanh, 2 hàng cho ván tiêu chuẩn).",
                ),
            ),
            OnboardingStep(
                stepNumber = "02 / 04",
                title = "Quân bài Jack đặc biệt",
                subtitle = "J 2 MẮT & J 1 MẮT",
                icon = LineIcon.Chip,
                iconBgColor = BrandRed.copy(alpha = 0.15f),
                iconColor = BrandRed,
                points = listOf(
                    "J 2 MẮT (J rô ♦, J chuồn ♣)" to "Đặt 1 quân cờ vào bất kỳ ô trống nào còn lại trên bàn cờ.",
                    "J 1 MẮT (J cơ ♥, J bích ♠)" to "Bắn tỉa và gỡ bỏ 1 quân cờ của đối phương (ngoại trừ các quân đã nằm trong chuỗi 5 đã khóa).",
                    "ĐỔI BÀI CHẾT" to "Khi một lá bài trên tay có cả 2 ô trên bàn đều đã bị chiếm, chọn lá đó và bấm 'ĐỔI BÀI CHẾT' để rút lá mới từ bộ bài.",
                ),
            ),
            OnboardingStep(
                stepNumber = "03 / 04",
                title = "Ghép bài chiến thuật",
                subtitle = "TACTICAL CRAFTING",
                icon = LineIcon.Bolt,
                iconBgColor = GoldAccent.copy(alpha = 0.18f),
                iconColor = BrandDark,
                points = listOf(
                    "GHÉP ĐÔI (2 lá cùng số)" to "Ghép 2 lá bài cùng số (ví dụ: đôi 7, đôi K) để tạo ra một Jack 2 mắt nhân tạo, cho phép đặt quân ở bất kỳ ô trống nào.",
                    "ĐỒNG CHẤT LIỀN KỀ" to "Ghép 2 lá bài liên tiếp cùng chất (ví dụ: 8♠ + 9♠) để tạo ra một Jack 1 mắt nhân tạo, cho phép bắn tỉa 1 quân của đối thủ.",
                    "TIẾT KIỆM TÀI NGUYÊN" to "Dùng 2 lá bài trên tay để tạo nước đi chiến lược, sau đó bạn sẽ được rút lại đủ 2 lá bài mới.",
                ),
            ),
            OnboardingStep(
                stepNumber = "04 / 04",
                title = "Thiên Phạt Hoàng Kim",
                subtitle = "DIVINE WIPE",
                icon = LineIcon.Sparkle,
                iconBgColor = BrandRed.copy(alpha = 0.15f),
                iconColor = BrandRed,
                points = listOf(
                    "SẢNH ĐỒNG CHẤT 5 LÁ" to "Khi trên tay bạn tập hợp đủ 5 lá bài sảnh đồng chất liên tiếp (Straight Flush), bạn đã mở khóa chiêu thức tối thượng.",
                    "QUÉT SẠCH BÀN CỜ" to "Nút 'KÍCH HOẠT THIÊN PHẠT' sẽ rực sáng. Khi bấm, toàn bộ quân cờ của đối thủ trên bàn cờ sẽ bị xóa sạch hoàn toàn!",
                    "LẬT NGƯỢC THẾ TRẬN" to "Đây là vũ khí chiến lược huyền thoại giúp lật ngược thế cờ ngay cả khi đối thủ sắp chiến thắng.",
                ),
            ),
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.lastIndex

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HƯỚNG DẪN NHANH · ${step.stepNumber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = BrandRed,
                    )
                    Text(
                        text = step.title,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .semantics { contentDescription = "Đóng hướng dẫn" },
                    contentAlignment = Alignment.Center,
                ) {
                    LineIconView(
                        icon = LineIcon.Close,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Step Content
            AnimatedContent(
                targetState = currentStepIndex,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { width -> width * direction } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width * direction } + fadeOut())
                },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) { targetIndex ->
                val current = steps[targetIndex]
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(current.iconBgColor)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            LineIconView(
                                icon = current.icon,
                                color = current.iconColor,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.4f,
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = current.subtitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = current.iconColor,
                            )
                            Text(
                                text = current.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    current.points.forEach { (label, detail) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(BrandRed),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                if (label != null) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                Text(
                                    text = detail,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Step Indicator Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                steps.indices.forEach { index ->
                    val isSelected = index == currentStepIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 22.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) BrandRed else MaterialTheme.colorScheme.outlineVariant)
                            .clickable { currentStepIndex = index }
                            .semantics {
                                contentDescription = "Trang ${index + 1} trên ${steps.size}"
                            },
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentStepIndex > 0) {
                    TextButton(
                        onClick = { currentStepIndex-- },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = "QUAY LẠI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = "BỎ QUA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isLastStep) {
                            onComplete()
                        } else {
                            currentStepIndex++
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        text = if (isLastStep) "BẮT ĐẦU CHƠI" else "TIẾP THEO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LineIconView(
                        icon = LineIcon.ArrowRight,
                        color = PureWhite,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

