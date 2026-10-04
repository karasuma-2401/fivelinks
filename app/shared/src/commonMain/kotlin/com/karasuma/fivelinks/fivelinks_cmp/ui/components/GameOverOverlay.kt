package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

/**
 * End-of-match screen modelled on the reference's win screen: cards rain down
 * around the edges while the match time takes centre stage.
 */
@Composable
fun GameOverOverlay(
    gameState: GameState,
    elapsedSeconds: Int,
    onRematchClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winner = gameState.winner
    val humans = gameState.players.filter { !it.isAi }
    val winners = gameState.players.filter { it.team == winner }
    val playerLost = humans.size == 1 && winner != null && humans.none { it.team == winner }
    val headline = when {
        winner == null -> "HÒA TRẬN"
        playerLost -> "THẤT BẠI"
        else -> "CHIẾN THẮNG"
    }
    val sequences = winner?.let { gameState.sequencesOf(it) } ?: 0

    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        intro.animateTo(1f, tween(durationMillis = 1300, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Swallow taps so nothing reaches the board underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        ScatteredCards(progress = { intro.value })

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .safeDrawingPadding()
                .widthIn(max = 420.dp)
                .padding(horizontal = 48.dp)
                .graphicsLayer {
                    val t = ((intro.value - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    alpha = t
                    translationY = (1f - t) * 24.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KẾT THÚC TRẬN ĐẤU",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = headline,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            if (winners.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(winner!!.primaryColor())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = winners.joinToString(" & ") { it.name }.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = winner.primaryColor()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = formatElapsed(elapsedSeconds),
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    )
                ),
                color = BrandRed
            )
            Text(
                text = "THỜI GIAN TRẬN ĐẤU",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StarDivider(modifier = Modifier.padding(vertical = 18.dp))

            Text(
                text = "${gameState.turnNumber} LƯỢT  ·  $sequences/${gameState.config.sequenceToWin} HÀNG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onRematchClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                elevation = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(14.dp, CircleShape, ambientColor = BrandRed, spotColor = BrandRed)
            ) {
                Text(
                    text = "CHƠI LẠI",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onMenuClick,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "VỀ MENU",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StarDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val line = MaterialTheme.colorScheme.outline
        Box(modifier = Modifier.width(44.dp).height(1.dp).background(line))
        CornerStarVector(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(14.dp),
            color = GoldAccent
        )
        Box(modifier = Modifier.width(44.dp).height(1.dp).background(line))
    }
}

private class ScatterSpot(val x: Float, val y: Float, val rotation: Float, val card: Card)

// Kept clear of the centre column (x 0.1..0.9, y 0.22..0.72) and the status bar;
// side cards only peek in from the edges.
private val ScatterSpots = listOf(
    ScatterSpot(0.03f, 0.06f, -24f, Card(Suit.HEARTS, Rank.KING)),
    ScatterSpot(0.26f, 0.045f, 18f, Card(Suit.SPADES, Rank.ACE)),
    ScatterSpot(0.56f, 0.055f, -12f, Card(Suit.DIAMONDS, Rank.QUEEN)),
    ScatterSpot(0.80f, 0.1f, 28f, Card(Suit.CLUBS, Rank.JACK)),
    ScatterSpot(-0.08f, 0.3f, 38f, Card(Suit.SPADES, Rank.TEN)),
    ScatterSpot(0.93f, 0.34f, -20f, Card(Suit.HEARTS, Rank.NINE)),
    ScatterSpot(-0.07f, 0.56f, -14f, Card(Suit.DIAMONDS, Rank.SEVEN)),
    ScatterSpot(0.92f, 0.6f, 22f, Card(Suit.SPADES, Rank.KING)),
    ScatterSpot(0.05f, 0.76f, 26f, Card(Suit.CLUBS, Rank.FIVE)),
    ScatterSpot(0.82f, 0.78f, -30f, Card(Suit.HEARTS, Rank.ACE)),
    ScatterSpot(0.22f, 0.85f, -18f, Card(Suit.SPADES, Rank.QUEEN)),
    ScatterSpot(0.46f, 0.87f, 12f, Card(Suit.DIAMONDS, Rank.TEN)),
    ScatterSpot(0.68f, 0.84f, -8f, Card(Suit.CLUBS, Rank.KING))
)

/** Cards that fall from above and settle around the screen edges. */
@Composable
private fun ScatteredCards(progress: () -> Float) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cardWidth = 50.dp
        ScatterSpots.forEachIndexed { index, spot ->
            PlayingCardFace(
                card = spot.card,
                width = cardWidth,
                showJackRole = false,
                modifier = Modifier
                    .offset(x = maxWidth * spot.x, y = maxHeight * spot.y)
                    .graphicsLayer {
                        val local = ((progress() - index * 0.035f) / 0.55f).coerceIn(0f, 1f)
                        val eased = FastOutSlowInEasing.transform(local)
                        translationY = -(1f - eased) * size.height * 6f
                        rotationZ = spot.rotation + (1f - eased) * 70f
                        alpha = eased
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(cardWidth * 0.14f)
                    }
            )
        }
    }
}
