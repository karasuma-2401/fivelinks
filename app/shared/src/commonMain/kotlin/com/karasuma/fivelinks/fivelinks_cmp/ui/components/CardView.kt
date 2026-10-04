package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.isJack
import com.karasuma.fivelinks.fivelinks_cmp.domain.isTwoEyedJack
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite

val HandCardWidth = 68.dp

/**
 * Flat card face in the reference style: coral for red suits, charcoal for
 * black suits, white print. Every measure derives from [width]; text is
 * converted from dp so the system font scale can never overflow the card.
 */
@Composable
fun PlayingCardFace(
    card: Card,
    width: Dp,
    modifier: Modifier = Modifier,
    showJackRole: Boolean = true
) {
    val density = LocalDensity.current
    val indexRankSize = with(density) { (width * 0.27f).toSp() }
    val cornerRankSize = with(density) { (width * 0.17f).toSp() }
    val shape = RoundedCornerShape(width * 0.14f)
    val edge = FiveLinksTheme.colors.cardEdge

    Box(
        modifier = modifier
            .size(width, width * 1.5f)
            .clip(shape)
            .background(card.suit.cardBackground())
            .then(if (edge.alpha > 0f) Modifier.border(1.dp, edge, shape) else Modifier)
    ) {
        CardIndex(
            card = card,
            rankSize = indexRankSize,
            suitSize = width * 0.17f,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = width * 0.08f, top = width * 0.07f)
        )

        if (showJackRole && card.isJack()) {
            // A jack's centre shows what it does instead of its suit.
            LineIconView(
                icon = card.jackRoleIcon(),
                color = card.suit.color(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width * 0.36f)
            )
        } else {
            SuitVector(
                suit = card.suit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width * 0.4f),
                color = card.suit.color()
            )
        }

        CardIndex(
            card = card,
            rankSize = cornerRankSize,
            suitSize = width * 0.11f,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = width * 0.08f, bottom = width * 0.07f)
                .rotate(180f)
        )
    }
}

@Composable
private fun CardIndex(
    card: Card,
    rankSize: TextUnit,
    suitSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.rank.shortName(),
            fontSize = rankSize,
            lineHeight = rankSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = card.suit.color(),
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(suitSize * 0.2f))
        SuitVector(
            suit = card.suit,
            modifier = Modifier.size(suitSize),
            color = card.suit.color()
        )
    }
}

/**
 * What a jack does: sparkle for a two-eyed jack (place anywhere), crosshair for a
 * one-eyed jack (snipe). It sits in the part of the card that stays visible while
 * the hand overlaps; the rules dialog explains both.
 */
private fun Card.jackRoleIcon(): LineIcon = if (isTwoEyedJack()) LineIcon.Sparkle else LineIcon.Crosshair

@Composable
fun HandCardView(
    card: Card,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = HandCardWidth,
    isDead: Boolean = false,
    isCraftHighlight: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val lift by animateDpAsState(
        targetValue = if (isSelected) (-16).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
    )
    val shape = RoundedCornerShape(width * 0.14f)
    val (borderWidth, borderColor) = when {
        isCraftHighlight -> 2.5.dp to GoldAccent
        isSelected -> 2.dp to PureWhite
        else -> 1.dp to PureWhite.copy(alpha = 0.16f)
    }

    Box(
        modifier = modifier
            .offset(y = lift)
            .shadow(if (isSelected) 14.dp else 5.dp, shape)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        PlayingCardFace(card = card, width = width)

        if (isDead) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                val deadSize = with(LocalDensity.current) { (width * 0.13f).toSp() }
                Text(
                    text = "CHẾT",
                    fontSize = deadSize,
                    lineHeight = deadSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = PureWhite,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = width * 0.08f, bottom = width * 0.12f)
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(borderWidth, borderColor, shape)
        )
    }
}

/** Small card (rank over suit) used inline in banners and the tactical bar. */
@Composable
fun MiniCard(
    card: Card,
    modifier: Modifier = Modifier,
    width: Dp = 26.dp
) {
    val shape = RoundedCornerShape(width * 0.22f)
    val edge = FiveLinksTheme.colors.cardEdge
    Box(
        modifier = modifier
            .size(width, width * 1.25f)
            .clip(shape)
            .background(card.suit.cardBackground())
            .then(if (edge.alpha > 0f) Modifier.border(1.dp, edge, shape) else Modifier)
    ) {
        BoardCellCardView(card = card, cellSize = width * 1.08f)
    }
}

/** Rank + suit printed on a board cell; the cell itself paints the card stock. */
@Composable
fun BoardCellCardView(
    card: Card,
    cellSize: Dp,
    modifier: Modifier = Modifier
) {
    val rankSize = with(LocalDensity.current) { (cellSize * 0.34f).toSp() }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.rank.shortName(),
            fontSize = rankSize,
            lineHeight = rankSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            color = card.suit.color(),
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(cellSize * 0.05f))
        SuitVector(
            suit = card.suit,
            modifier = Modifier.size(cellSize * 0.24f),
            color = card.suit.color()
        )
    }
}
