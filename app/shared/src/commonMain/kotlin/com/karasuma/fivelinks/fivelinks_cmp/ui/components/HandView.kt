package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

@Composable
fun HandView(
    hand: Hand,
    owner: Player,
    selectedIndices: Set<Int>,
    gameState: GameState,
    isInteractive: Boolean,
    onCardClick: (Int) -> Unit,
    onSwapDeadCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    fun isCardDead(card: Card): Boolean {
        if (card.isJack()) return false
        val positions = gameState.board.positionsOf(card)
        if (positions.isEmpty()) return false
        return positions.all { gameState.chips.contains(it) } && !gameState.deck.isEmpty()
    }

    val singleSelectedIndex = if (selectedIndices.size == 1) selectedIndices.first() else null
    val deadCardIndex = singleSelectedIndex?.takeIf { isInteractive && isCardDead(hand[it]) }
    // The hand stays visible during the opponent's turn, just quieter.
    val handAlpha by animateFloatAsState(if (isInteractive) 1f else 0.6f)
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = deadCardIndex != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Button(
                onClick = { deadCardIndex?.let(onSwapDeadCard) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .height(36.dp)
            ) {
                Text(
                    text = "ĐỔI BÀI CHẾT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Fanned hand: cards overlap like physical cards and follow a gentle arc.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(HandCardWidth * 1.5f + 20.dp)
                .alpha(handAlpha)
        ) {
            val totalCards = hand.cards.size
            if (totalCards > 0) {
                val cardWidth = HandCardWidth
                val available = maxWidth - cardWidth - 56.dp
                val idealStep = 44.dp
                val step = if (totalCards > 1) minOf(idealStep, available / (totalCards - 1)) else idealStep
                val startX = (maxWidth - (cardWidth + step * (totalCards - 1))) / 2
                val middle = (totalCards - 1) / 2f

                hand.cards.forEachIndexed { index, card ->
                    val isSelected = index in selectedIndices
                    val fromMiddle = index - middle
                    HandCardView(
                        card = card,
                        isSelected = isSelected,
                        isDead = isCardDead(card),
                        isCraftHighlight = isSelected && selectedIndices.size >= 2,
                        enabled = isInteractive,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onCardClick(index)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = startX + step * index, y = (fromMiddle * fromMiddle * 1.4f).dp - 10.dp)
                            .zIndex(if (isSelected) 50f else index.toFloat())
                            .graphicsLayer {
                                rotationZ = fromMiddle * 2.6f
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Whose hand this is (matters in pass-and-play).
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(owner.team.primaryColor())
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = owner.name.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
