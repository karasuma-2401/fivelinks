package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRed

@Composable
fun HandView(
    hand: Hand,
    selectedIndices: Set<Int>,
    gameState: GameState,
    onCardClick: (Int) -> Unit,
    onSwapDeadCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Helper to check if a specific card in hand is dead
    fun isCardDead(card: Card): Boolean {
        if (card.isJack()) return false
        val positions = gameState.board.positionsOf(card)
        if (positions.isEmpty()) return false
        return positions.all { gameState.chips.contains(it) } && !gameState.deck.isEmpty()
    }

    val singleSelectedIndex = if (selectedIndices.size == 1) selectedIndices.first() else null
    val selectedIsDead = singleSelectedIndex?.let { isCardDead(hand[it]) } == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(SurfaceDark.copy(alpha = 0.95f))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hand cards horizontal row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            hand.cards.forEachIndexed { index, card ->
                val isSelected = index in selectedIndices
                val isDead = isCardDead(card)

                HandCardView(
                    card = card,
                    isSelected = isSelected,
                    isDead = isDead,
                    isCraftHighlight = isSelected && selectedIndices.size >= 2,
                    onClick = { onCardClick(index) },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Action button for dead card swap if selected
        if (selectedIsDead) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { singleSelectedIndex?.let { onSwapDeadCard(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = TeamRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "♻ Đổi bài chết (Rút lá mới)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
