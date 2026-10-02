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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: "Your turn" + "DECK 80   TURN 10" + sequence score dots
        Column {
            Text(
                text = if (currentPlayer.isAi) "AI Turn" else "Your turn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DECK ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "TURN ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(14.dp))

                // Sequence scores: TeamBlue count vs TeamRed count with native colored dots
                val target = gameState.config.sequenceToWin
                val blueCount = gameState.sequencesOf(Team.BLUE)
                val redCount = gameState.sequencesOf(Team.RED)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TeamBlue)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "/",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TeamRed)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "/",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
        }

        // Right: Pill Badge "YOUR TURN" / "AI THINKING" + Sleek Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badgeShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(badgeShape)
                    .background(TurnBadgeBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAiThinking) "AI THINKING" else "YOUR TURN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAiThinking) GoldAccent.copy(alpha = pulseAlpha) else TurnBadgeText
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Rules button
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
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Restart button
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
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Menu button
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
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            )
        }
    }
}
