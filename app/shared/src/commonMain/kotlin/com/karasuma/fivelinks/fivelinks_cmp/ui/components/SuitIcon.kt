package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardSuitBlack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardSuitRed

fun Suit.symbol(): String = when (this) {
    Suit.HEARTS -> "♥"
    Suit.DIAMONDS -> "♦"
    Suit.CLUBS -> "♣"
    Suit.SPADES -> "♠"
}

fun Suit.color(): Color = when (this) {
    Suit.HEARTS, Suit.DIAMONDS -> CardSuitRed
    Suit.CLUBS, Suit.SPADES -> CardSuitBlack
}

fun Rank.shortName(): String = when (this) {
    Rank.TWO -> "2"
    Rank.THREE -> "3"
    Rank.FOUR -> "4"
    Rank.FIVE -> "5"
    Rank.SIX -> "6"
    Rank.SEVEN -> "7"
    Rank.EIGHT -> "8"
    Rank.NINE -> "9"
    Rank.TEN -> "10"
    Rank.JACK -> "J"
    Rank.QUEEN -> "Q"
    Rank.KING -> "K"
    Rank.ACE -> "A"
}

@Composable
fun SuitText(
    suit: Suit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp
) {
    Text(
        text = suit.symbol(),
        color = suit.color(),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}
