package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.isOneEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.domain.isTwoEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardBorder
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardSurfaceLight
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.HighlightSelected
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRed

@Composable
fun HandCardView(
    card: Card,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDead: Boolean = false,
    isCraftHighlight: Boolean = false,
    onClick: () -> Unit = {}
) {
    val offsetY by animateDpAsState(
        targetValue = if (isSelected) (-12).dp else 0.dp,
        animationSpec = tween(durationMillis = 150)
    )

    val borderColor = when {
        isCraftHighlight -> GoldAccent
        isSelected -> HighlightSelected
        else -> CardBorder
    }

    val borderWidth = when {
        isCraftHighlight -> 2.5.dp
        isSelected -> 2.dp
        else -> 1.dp
    }

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .shadow(if (isSelected) 8.dp else 2.dp, shape)
            .clip(shape)
            .background(if (isDead) Color(0xFFE2E8F0) else CardSurfaceLight)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
            .width(54.dp)
            .height(80.dp)
    ) {
        // Top-left Rank & Suit
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.shortName(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = card.suit.color()
            )
            Text(
                text = card.suit.symbol(),
                fontSize = 10.sp,
                color = card.suit.color()
            )
        }

        // Center: Big Suit or Special Jack badge
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                card.isOneEyedJack() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎯",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "1-EYE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TeamRed
                        )
                    }
                }
                card.isTwoEyedJack() -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👑",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "2-EYE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TeamBlue
                        )
                    }
                }
                else -> {
                    Text(
                        text = card.suit.symbol(),
                        fontSize = 22.sp,
                        color = card.suit.color()
                    )
                }
            }
        }

        // Bottom-right Rank & Suit (rotated 180 degrees)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(180f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.shortName(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = card.suit.color()
            )
            Text(
                text = card.suit.symbol(),
                fontSize = 10.sp,
                color = card.suit.color()
            )
        }

        // Dead Card Overlay
        if (isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DEAD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BoardCellCardView(
    card: Card,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.rank.shortName(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = card.suit.color(),
            lineHeight = 11.sp
        )
        Text(
            text = card.suit.symbol(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = card.suit.color(),
            lineHeight = 11.sp
        )
    }
}
