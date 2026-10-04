package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardBgBlack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardBgRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CardContentWhite

fun Suit.symbol(): String = when (this) {
    Suit.HEARTS -> "♥"
    Suit.DIAMONDS -> "♦"
    Suit.CLUBS -> "♣"
    Suit.SPADES -> "♠"
}

fun Suit.isRed(): Boolean = this == Suit.HEARTS || this == Suit.DIAMONDS

/** Card stock colour: coral for red suits, charcoal for black suits. */
fun Suit.cardBackground(): Color = if (isRed()) CardBgRed else CardBgBlack

/** Print colour on the card stock (always white, as in the reference). */
fun Suit.color(): Color = CardContentWhite

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

/**
 * Pure vector representation of playing card suits (Diamond, Heart, Spade, Club).
 * Uses Compose Canvas so it does not rely on OS or web fonts, guaranteeing
 * 100% crisp, bug-free rendering across Web (Wasm), Android, iOS, and Desktop.
 */
@Composable
fun SuitVector(
    suit: Suit,
    modifier: Modifier = Modifier,
    color: Color = suit.color()
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 24f
        val offsetX = (size.width - 24f * scale) / 2f
        val offsetY = (size.height - 24f * scale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, Offset.Zero)
        }) {
            when (suit) {
                Suit.DIAMONDS -> {
                    val path = Path().apply {
                        moveTo(12f, 2f)
                        lineTo(21f, 12f)
                        lineTo(12f, 22f)
                        lineTo(3f, 12f)
                        close()
                    }
                    drawPath(path, color)
                }
                Suit.HEARTS -> {
                    val path = Path().apply {
                        moveTo(12f, 7.5f)
                        cubicTo(10f, 3f, 4f, 3.5f, 3f, 8.5f)
                        cubicTo(2f, 13.5f, 7.5f, 17.5f, 12f, 22f)
                        cubicTo(16.5f, 17.5f, 22f, 13.5f, 21f, 8.5f)
                        cubicTo(20f, 3.5f, 14f, 3f, 12f, 7.5f)
                        close()
                    }
                    drawPath(path, color)
                }
                Suit.SPADES -> {
                    val path = Path().apply {
                        moveTo(12f, 2f)
                        cubicTo(10f, 6.5f, 2.5f, 10f, 2.5f, 14.5f)
                        cubicTo(2.5f, 18f, 6.5f, 19.5f, 10f, 17.5f)
                        lineTo(9.5f, 22f)
                        lineTo(14.5f, 22f)
                        lineTo(14f, 17.5f)
                        cubicTo(17.5f, 19.5f, 21.5f, 18f, 21.5f, 14.5f)
                        cubicTo(21.5f, 10f, 14f, 6.5f, 12f, 2f)
                        close()
                    }
                    drawPath(path, color)
                }
                Suit.CLUBS -> {
                    drawCircle(color, radius = 4.2f, center = Offset(12f, 7.5f))
                    drawCircle(color, radius = 4.2f, center = Offset(7.5f, 14.5f))
                    drawCircle(color, radius = 4.2f, center = Offset(16.5f, 14.5f))
                    drawCircle(color, radius = 3.5f, center = Offset(12f, 12.5f))
                    val stem = Path().apply {
                        moveTo(10.5f, 13f)
                        lineTo(8.5f, 22f)
                        lineTo(15.5f, 22f)
                        lineTo(13.5f, 13f)
                        close()
                    }
                    drawPath(stem, color)
                }
            }
        }
    }
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
