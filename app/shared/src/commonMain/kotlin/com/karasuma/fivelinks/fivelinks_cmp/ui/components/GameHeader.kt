package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BackgroundDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TextPrimary
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TextSecondary
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TurnBadgeBg
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TurnBadgeText

@Composable
fun GameHeader(
    gameState: GameState,
    isAiThinking: Boolean,
    onRestartClick: () -> Unit,
    onRulesClick: () -> Unit = {},
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer
    val deckCount = gameState.deck.size
    val turnNumber = gameState.turnNumber + 1

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val target = gameState.config.sequenceToWin
    val blueCount = gameState.sequencesOf(Team.BLUE)
    val redCount = gameState.sequencesOf(Team.RED)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Row: Player turn headline + Status badge + Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (currentPlayer.isAi) "AI Turn" else "Your turn",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                val badgeShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .clip(badgeShape)
                        .background(TurnBadgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAiThinking) "AI THINKING" else "YOUR TURN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAiThinking) GoldAccent.copy(alpha = pulseAlpha) else TurnBadgeText
                    )
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Luật",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRulesClick
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = "Lại",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRestartClick
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = "Menu",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMenuClick
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub Row: Match Telemetry & Dynamic Sequence Progress Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DECK $deckCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "TURN $turnNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            // Sequence scores
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(TeamBlue)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$blueCount/$target",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(TeamRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$redCount/$target",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}
