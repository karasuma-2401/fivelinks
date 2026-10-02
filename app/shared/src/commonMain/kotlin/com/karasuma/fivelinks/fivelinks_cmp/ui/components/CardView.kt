package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardSurfaceWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
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
        targetValue = if (isSelected) (-18).dp else 0.dp,
        animationSpec = tween(durationMillis = 150)
    )

    val borderColor = when {
        isCraftHighlight -> GoldAccent
        isSelected -> GoldAccent
        else -> CardBorder
    }

    val borderWidth = when {
        isCraftHighlight || isSelected -> 2.dp
        else -> 1.dp
    }

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .shadow(if (isSelected) 10.dp else 4.dp, shape)
            .clip(shape)
            .background(CardSurfaceWhite)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
            .width(52.dp)
            .height(82.dp)
    ) {
        // Top-left Rank & Vector Suit
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.shortName(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = card.suit.color(),
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(1.dp))
            SuitVector(
                suit = card.suit,
                modifier = Modifier.size(10.dp),
                color = card.suit.color()
            )
        }

        // Center: Vector Suit Watermark or Jack badge
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                card.isOneEyedJack() -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TeamRed.copy(alpha = 0.12f))
                            .border(1.dp, TeamRed.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SNIPE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TeamRed
                        )
                    }
                }
                card.isTwoEyedJack() -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TeamBlue.copy(alpha = 0.12f))
                            .border(1.dp, TeamBlue.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WILD",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TeamBlue
                        )
                    }
                }
                else -> {
                    SuitVector(
                        suit = card.suit,
                        modifier = Modifier.size(24.dp),
                        color = card.suit.color().copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Bottom-right Inverted Rank & Vector Suit
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(180f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.rank.shortName(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = card.suit.color(),
                lineHeight = 13.sp,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(1.dp))
            SuitVector(
                suit = card.suit,
                modifier = Modifier.size(9.dp),
                color = card.suit.color()
            )
        }

        // Dead Card Dimming Overlay
        if (isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
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
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.rank.shortName(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = card.suit.color(),
            lineHeight = 11.sp,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(1.5.dp))
        SuitVector(
            suit = card.suit,
            modifier = Modifier.size(9.dp),
            color = card.suit.color()
        )
    }
}
