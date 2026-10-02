package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlueGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRedGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.TacticalAction

@Composable
fun TacticalCraftingBar(
    tacticalAction: TacticalAction?,
    onTriggerDivineWipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    AnimatedVisibility(
        visible = tacticalAction != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(12.dp)

        when (tacticalAction) {
            is TacticalAction.PairWild -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .shadow(8.dp, shape)
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1E3A8A).copy(alpha = 0.9f),
                                    Color(0xFF0F172A).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(1.5.dp, TeamBlueGlow.copy(alpha = pulseAlpha), shape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⚡ GHÉP ĐÔI: JACK 2 MẮT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TeamBlueGlow
                            )
                            Text(
                                text = "Nhấp vào bất kỳ ô trống nào trên bàn để đặt quân!",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Text(
                            text = "👑 WILD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }

            is TacticalAction.ConnectorSnipe -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .shadow(8.dp, shape)
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF881337).copy(alpha = 0.9f),
                                    Color(0xFF0F172A).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(1.5.dp, TeamRedGlow.copy(alpha = pulseAlpha), shape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎯 SUITED CONNECTOR: JACK 1 MẮT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TeamRedGlow
                            )
                            Text(
                                text = "Nhấp vào 1 quân cờ đối thủ (chưa khóa) để bắn tỉa!",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Text(
                            text = "💥 SNIPE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TeamRedGlow
                        )
                    }
                }
            }

            is TacticalAction.DivineWipe -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .shadow(14.dp, shape)
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF78350F).copy(alpha = 0.95f),
                                    Color(0xFF451A03).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(2.dp, GoldGlow.copy(alpha = pulseAlpha), shape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "👑 THIÊN PHẠT HOÀNG KIM 👑",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldGlow
                            )
                            Text(
                                text = "Sảnh 5 lá đồng chất: Xóa sạch toàn bộ quân cờ của đối thủ!",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onTriggerDivineWipe,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "KÍCH HOẠT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            null -> Unit
        }
    }
}
