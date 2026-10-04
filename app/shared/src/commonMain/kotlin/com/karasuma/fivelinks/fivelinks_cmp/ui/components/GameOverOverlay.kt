package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * End-of-match screen modelled on the reference's win screen: cards rain down
 * and circle the edges while the match time takes centre stage.
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
        OrbitingCards(intro = { intro.value })

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

private val OrbitCards = listOf(
    Card(Suit.HEARTS, Rank.KING) to -12f,
    Card(Suit.SPADES, Rank.ACE) to 9f,
    Card(Suit.DIAMONDS, Rank.QUEEN) to -6f,
    Card(Suit.CLUBS, Rank.JACK) to 14f,
    Card(Suit.SPADES, Rank.TEN) to -16f,
    Card(Suit.HEARTS, Rank.NINE) to 8f,
    Card(Suit.DIAMONDS, Rank.SEVEN) to -10f,
    Card(Suit.SPADES, Rank.KING) to 12f,
    Card(Suit.CLUBS, Rank.FIVE) to -8f,
    Card(Suit.HEARTS, Rank.ACE) to 16f,
    Card(Suit.SPADES, Rank.QUEEN) to -14f,
    Card(Suit.DIAMONDS, Rank.TEN) to 6f,
    Card(Suit.CLUBS, Rank.KING) to -9f
)

/** One full lap takes this long: slow enough to stay in the background. */
private const val OrbitPeriodMillis = 50_000

/**
 * Cards drop in, then circle the result clockwise along a rounded-rectangle loop
 * on the screen edges (side cards only peek in), so they never cover the text or the buttons.
 * Positions are only read while drawing, so the orbit causes no recomposition.
 */
@Composable
private fun OrbitingCards(intro: () -> Float) {
    val orbit = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = OrbitPeriodMillis, easing = LinearEasing))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val cardWidth = 50.dp
        val loop = with(density) {
            OrbitLoop(
                left = 0f,
                top = 74.dp.toPx(),
                right = maxWidth.toPx(),
                bottom = maxHeight.toPx() - 74.dp.toPx(),
                radius = 96.dp.toPx()
            )
        }
        val halfWidth = with(density) { cardWidth.toPx() } / 2f
        val halfHeight = halfWidth * 1.5f
        val dropDistance = with(density) { maxHeight.toPx() } * 0.7f

        OrbitCards.forEachIndexed { index, (card, tilt) ->
            PlayingCardFace(
                card = card,
                width = cardWidth,
                showJackRole = false,
                modifier = Modifier.graphicsLayer {
                    val point = loop.pointAt((index.toFloat() / OrbitCards.size + orbit.value) * loop.length)
                    val local = ((intro() - index * 0.035f) / 0.55f).coerceIn(0f, 1f)
                    val eased = FastOutSlowInEasing.transform(local)
                    translationX = point.x - halfWidth
                    translationY = point.y - halfHeight - (1f - eased) * dropDistance
                    rotationZ = point.angle + tilt + (1f - eased) * 70f
                    alpha = eased
                    shadowElevation = 8.dp.toPx()
                    shape = RoundedCornerShape(cardWidth * 0.14f)
                }
            )
        }
    }
}

private class LoopPoint(val x: Float, val y: Float, val angle: Float)

/** A clockwise rounded-rectangle loop walked at constant speed. */
private class OrbitLoop(
    private val left: Float,
    private val top: Float,
    private val right: Float,
    private val bottom: Float,
    radius: Float
) {
    private val r = minOf(radius, (right - left) / 2f, (bottom - top) / 2f).coerceAtLeast(1f)
    private val straightH = right - left - 2 * r
    private val straightV = bottom - top - 2 * r
    private val arc = PI.toFloat() / 2f * r
    val length = 2 * straightH + 2 * straightV + 4 * arc

    /** Position and heading (degrees, 0 = moving right) at [distance] along the loop. */
    fun pointAt(distance: Float): LoopPoint {
        var d = distance % length
        if (d < 0f) d += length
        if (d < straightH) return LoopPoint(left + r + d, top, 0f)
        d -= straightH
        if (d < arc) return corner(right - r, top + r, -90f, d)
        d -= arc
        if (d < straightV) return LoopPoint(right, top + r + d, 90f)
        d -= straightV
        if (d < arc) return corner(right - r, bottom - r, 0f, d)
        d -= arc
        if (d < straightH) return LoopPoint(right - r - d, bottom, 180f)
        d -= straightH
        if (d < arc) return corner(left + r, bottom - r, 90f, d)
        d -= arc
        if (d < straightV) return LoopPoint(left, bottom - r - d, 270f)
        d -= straightV
        return corner(left + r, top + r, 180f, d)
    }

    /** Point [distance] into the quarter arc around (cx, cy) that starts at [startDegrees]. */
    private fun corner(cx: Float, cy: Float, startDegrees: Float, distance: Float): LoopPoint {
        val degrees = startDegrees + distance / r * (180f / PI.toFloat())
        val radians = degrees * PI.toFloat() / 180f
        return LoopPoint(cx + r * cos(radians), cy + r * sin(radians), degrees + 90f)
    }
}
