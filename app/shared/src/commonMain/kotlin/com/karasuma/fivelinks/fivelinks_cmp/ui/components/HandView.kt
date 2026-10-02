package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BackgroundDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TextPrimary
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

@Composable
fun HandView(
    hand: Hand,
    selectedIndices: Set<Int>,
    gameState: GameState,
    onCardClick: (Int) -> Unit,
    onSwapDeadCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer

    fun isCardDead(card: Card): Boolean {
        if (card.isJack()) return false
        val positions = gameState.board.positionsOf(card)
        if (positions.isEmpty()) return false
        return positions.all { gameState.chips.contains(it) } && !gameState.deck.isEmpty()
    }

    val singleSelectedIndex = if (selectedIndices.size == 1) selectedIndices.first() else null
    val deadCardIndex = singleSelectedIndex?.takeIf { isCardDead(hand[it]) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dead card swap action button
        if (deadCardIndex != null) {
            Button(
                onClick = { onSwapDeadCard(deadCardIndex) },
                colors = ButtonDefaults.buttonColors(containerColor = TeamRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(32.dp)
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Đổi bài chết",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Fanned Overlapping Playing Cards
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val totalCards = hand.cards.size
            val cardWidth = 52.dp
            val cardHeight = 82.dp

            if (totalCards > 0) {
                // Calculate step distance between cards so they overlap like physical cards
                val availableWidth = maxWidth - cardWidth - 24.dp
                val idealStep = 38.dp // ~14dp overlap
                val step = if (availableWidth > 0.dp && (totalCards - 1) > 0) {
                    val fitStep = availableWidth / (totalCards - 1)
                    if (fitStep < idealStep) fitStep else idealStep
                } else idealStep

                val totalHandWidth = cardWidth + step * (totalCards - 1)
                val startX = (maxWidth - totalHandWidth) / 2

                Box(modifier = Modifier.fillMaxWidth().height(104.dp)) {
                    hand.cards.forEachIndexed { index, card ->
                        val isSelected = index in selectedIndices
                        val isDead = isCardDead(card)
                        val posX = startX + (step * index)

                        Box(
                            modifier = Modifier
                                .offset(x = posX)
                                .align(Alignment.BottomStart)
                                .zIndex(if (isSelected) 50f else index.toFloat())
                        ) {
                            HandCardView(
                                card = card,
                                isSelected = isSelected,
                                isDead = isDead,
                                isCraftHighlight = isSelected && selectedIndices.size >= 2,
                                onClick = { onCardClick(index) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Player Indicator Bar (matching reference image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(currentPlayer.team.primaryColor())
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = currentPlayer.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
