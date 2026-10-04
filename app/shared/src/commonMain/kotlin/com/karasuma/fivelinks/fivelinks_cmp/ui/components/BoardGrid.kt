package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.ChipSequence
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun BoardGrid(
    gameState: GameState,
    validPlacementPositions: Set<BoardPosition>,
    validSnipePositions: Set<BoardPosition>,
    onCellClick: (BoardPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastMovePos: BoardPosition? = when (val lm = gameState.lastMove) {
        is Move.Place -> lm.position
        is Move.CraftPlace -> lm.position
        is Move.Remove -> lm.position
        is Move.CraftRemove -> lm.position
        else -> null
    }
    // While a card is selected everything that is not a legal target is dimmed.
    val hasTargets = validPlacementPositions.isNotEmpty() || validSnipePositions.isNotEmpty()

    // One shared pulse for every highlight; cells only read it while drawing.
    val pulse = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val colors = FiveLinksTheme.colors
    val haptic = LocalHapticFeedback.current

    // Chips removed by a snipe or by Thiên Phạt burst instead of vanishing, and
    // Thiên Phạt also sends a golden shockwave across the board.
    val scope = rememberCoroutineScope()
    val vanishing = remember { mutableStateOf<Map<Int, Team>>(emptyMap()) }
    val burstIsWipe = remember { mutableStateOf(false) }
    val vanish = remember { Animatable(1f) }
    val wave = remember { Animatable(1f) }
    val lastSeen = remember { arrayOf(gameState) }
    LaunchedEffect(gameState) {
        val before = lastSeen[0]
        lastSeen[0] = gameState
        val move = gameState.lastMove
        if (gameState.turnNumber <= before.turnNumber) return@LaunchedEffect
        if (move !is Move.Remove && move !is Move.CraftRemove && move !is Move.DivineWipe) return@LaunchedEffect

        val isWipe = move is Move.DivineWipe
        val gone = before.chips.chips.filterKeys { it !in gameState.chips.chips }
        // Launched outside this effect so the next move cannot cut the animation short.
        if (isWipe) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(durationMillis = 1800, easing = LinearEasing))
            }
        }
        if (gone.isNotEmpty()) {
            scope.launch {
                vanishing.value = gone
                burstIsWipe.value = isWipe
                vanish.snapTo(0f)
                vanish.animateTo(1f, tween(durationMillis = if (isWipe) 1400 else 650, easing = LinearEasing))
                vanishing.value = emptyMap()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boardDim = when {
            maxWidth > 10.dp && maxHeight > 10.dp -> minOf(maxWidth, maxHeight)
            maxWidth > 10.dp -> maxWidth
            else -> 320.dp
        }
        val framePadding = 5.dp
        val cellSize = (boardDim - framePadding * 2) / 10
        val frameShape = RoundedCornerShape(18.dp)

        Box(
            modifier = Modifier
                .size(boardDim)
                .graphicsLayer {
                    // A sharp, decaying shake at the start of the shockwave.
                    val shake = 1f - (wave.value / 0.4f).coerceIn(0f, 1f)
                    translationX = sin(wave.value * 70f) * shake * 7.dp.toPx()
                }
                .shadow(if (colors.isNight) 0.dp else 16.dp, frameShape)
                .clip(frameShape)
                .background(colors.boardFrame)
                .then(
                    if (colors.isNight) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, frameShape)
                    } else {
                        Modifier
                    }
                )
                .padding(framePadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 10) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (c in 0 until 10) {
                            // Shared instances keep the click lambdas stable across recompositions.
                            val pos = BoardPosition.all[r * 10 + c]
                            val isCorner = gameState.board.isCorner(pos)
                            val isValidPlacement = pos in validPlacementPositions
                            val isValidSnipe = pos in validSnipePositions

                            BoardCell(
                                card = if (isCorner) null else gameState.board.cardAt(pos),
                                chipTeam = gameState.chips.at(pos),
                                isCorner = isCorner,
                                isValidPlacement = isValidPlacement,
                                isValidSnipeTarget = isValidSnipe,
                                isDimmed = hasTargets && !isValidPlacement && !isValidSnipe,
                                isLastMove = pos == lastMovePos,
                                cellSize = cellSize,
                                pulse = pulse,
                                onClick = {
                                    if (isValidPlacement || isValidSnipe) {
                                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    }
                                    onCellClick(pos)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            SequenceLinks(
                sequences = gameState.completedSequence,
                modifier = Modifier.matchParentSize()
            )

            // Bursting chips: each swells and fades, rippling out from the centre.
            vanishing.value.forEach { (index, team) ->
                val row = index / 10
                val col = index % 10
                key(index) {
                    ChipView(
                        team = team,
                        animateIn = false,
                        modifier = Modifier
                            .offset(x = cellSize * col + 0.8.dp, y = cellSize * row + 0.8.dp)
                            .size(cellSize - 1.6.dp)
                            .graphicsLayer {
                                val t = burstProgress(vanish.value, row, col)
                                val scale = 1f + 0.9f * FastOutSlowInEasing.transform(t)
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - ((t - 0.25f) / 0.75f).coerceIn(0f, 1f)
                            }
                    )
                }
            }

            // Sparks from bursting chips, then the Thiên Phạt flash and shockwave.
            Canvas(modifier = Modifier.matchParentSize()) {
                val cell = size.width / 10f
                val gone = vanishing.value
                if (gone.isNotEmpty()) {
                    val sparkColor = if (burstIsWipe.value) GoldGlow else PureWhite
                    val progress = vanish.value
                    gone.keys.forEach { index ->
                        val row = index / 10
                        val col = index % 10
                        val t = burstProgress(progress, row, col)
                        if (t <= 0f || t >= 1f) return@forEach
                        val center = Offset((col + 0.5f) * cell, (row + 0.5f) * cell)
                        val reach = cell * (0.3f + 0.95f * LinearOutSlowInEasing.transform(t))
                        repeat(8) { k ->
                            val angle = k * (PI.toFloat() / 4f) + index * 0.7f
                            drawCircle(
                                color = sparkColor.copy(alpha = 1f - t),
                                radius = cell * 0.075f * (1f - t) + 1f,
                                center = center + Offset(cos(angle) * reach, sin(angle) * reach)
                            )
                        }
                    }
                }

                val t = wave.value
                if (t >= 1f) return@Canvas
                val maxRadius = size.maxDimension * 0.75f
                val flash = 1f - (t / 0.35f).coerceIn(0f, 1f)
                drawRect(GoldAccent.copy(alpha = 0.5f * flash * flash * flash))
                val ring = LinearOutSlowInEasing.transform((t / 0.75f).coerceIn(0f, 1f))
                val ringFade = 1f - ring
                // A soft glow under a bright core.
                listOf(3.2f to 0.18f, 1.8f to 0.35f, 1f to 1f).forEach { (widthScale, alpha) ->
                    drawCircle(
                        color = GoldGlow.copy(alpha = alpha * ringFade),
                        radius = maxRadius * ring,
                        style = Stroke(width = size.minDimension * 0.05f * widthScale * ringFade)
                    )
                }
                val echo = LinearOutSlowInEasing.transform(((t - 0.12f) / 0.75f).coerceIn(0f, 1f))
                if (echo > 0f) {
                    drawCircle(
                        color = PureWhite.copy(alpha = 0.8f * (1f - echo)),
                        radius = maxRadius * echo,
                        style = Stroke(width = size.minDimension * 0.02f * (1f - echo))
                    )
                }
            }

            WipeEmblem(
                progress = { wave.value },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/** A chip's own burst progress: chips near the centre go first. */
private fun burstProgress(progress: Float, row: Int, col: Int): Float {
    val delay = hypot(row - 4.5f, col - 4.5f) / 6.4f * BurstStagger
    return ((progress - delay) / (1f - BurstStagger)).coerceIn(0f, 1f)
}

/** "THIÊN PHẠT / HOÀNG KIM" emblem that punches in over the board, then fades. */
@Composable
private fun WipeEmblem(progress: () -> Float, modifier: Modifier = Modifier) {
    val glow = LocalTextStyle.current.merge(
        TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 4f), blurRadius = 18f))
    )
    Column(
        modifier = modifier.graphicsLayer {
            val t = progress()
            val appear = (t / 0.15f).coerceIn(0f, 1f)
            val leave = ((t - 0.6f) / 0.25f).coerceIn(0f, 1f)
            alpha = if (t >= 1f) 0f else appear * (1f - leave)
            val scale = 0.55f + 0.5f * FastOutSlowInEasing.transform(appear) + 0.12f * leave
            scaleX = scale
            scaleY = scale
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(18.dp, CircleShape)
                .clip(CircleShape)
                .background(GoldAccent),
            contentAlignment = Alignment.Center
        ) {
            LineIconView(icon = LineIcon.Bolt, color = BrandDark, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "THIÊN PHẠT",
            style = glow,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            color = GoldGlow
        )
        Text(
            text = "HOÀNG KIM",
            style = glow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            color = PureWhite
        )
    }
}

/** Share of the burst animation spent rippling from the centre to the corners. */
private const val BurstStagger = 0.35f

/** Draws a bar through the five chips of every completed sequence: the "five links". */
@Composable
private fun SequenceLinks(
    sequences: List<ChipSequence>,
    modifier: Modifier = Modifier
) {
    sequences.forEach { sequence ->
        key(sequence) {
            SequenceLink(sequence = sequence, modifier = modifier)
        }
    }
}

@Composable
private fun SequenceLink(
    sequence: ChipSequence,
    modifier: Modifier = Modifier
) {
    val ends = remember(sequence) {
        val ordered = sequence.positions.sortedWith(compareBy({ it.row }, { it.column }))
        ordered.first() to ordered.last()
    }
    // The bar is drawn from one end to the other when the sequence completes.
    val reveal = remember(sequence) { Animatable(0f) }
    LaunchedEffect(sequence) {
        reveal.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val cell = size.width / 10f
        fun centerOf(pos: BoardPosition) = Offset((pos.column + 0.5f) * cell, (pos.row + 0.5f) * cell)

        val start = centerOf(ends.first)
        val end = start + (centerOf(ends.second) - start) * reveal.value
        val drop = Offset(0f, cell * 0.06f)
        drawLine(Color.Black.copy(alpha = 0.28f), start + drop, end + drop, cell * 0.24f, StrokeCap.Round)
        drawLine(Color.White, start, end, cell * 0.2f, StrokeCap.Round)
        drawLine(sequence.team.primaryColor(), start, end, cell * 0.1f, StrokeCap.Round)
    }
}
