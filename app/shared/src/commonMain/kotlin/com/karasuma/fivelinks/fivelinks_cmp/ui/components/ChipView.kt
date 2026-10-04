package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.chipHighlight
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.chipShade
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Casino-style chip: a white rim with team-coloured edge spots around a glossy
 * body. The rim keeps a red chip readable even on a coral card cell.
 *
 * [pulse] is only read while drawing, so the last-move halo animates without
 * recomposing the board.
 */
@Composable
fun ChipView(
    team: Team,
    modifier: Modifier = Modifier,
    isLastMove: Boolean = false,
    pulse: State<Float>? = null,
    animateIn: Boolean = true
) {
    // Drop-in: the chip lands slightly oversized and settles with a small bounce.
    val drop = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) drop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            val t = drop.value
            scaleX = 1.35f - 0.35f * t
            scaleY = 1.35f - 0.35f * t
            alpha = t.coerceIn(0f, 1f)
        }
    ) {
        val outer = size.minDimension / 2f
        val radius = outer * 0.8f

        if (isLastMove) {
            drawCircle(
                color = Color.White.copy(alpha = pulse?.value ?: 1f),
                radius = outer - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        drawChip(team, center, radius)
    }
}

/** Draws one chip of [radius] at [center]; shared by [ChipView] and the shatter effect. */
internal fun DrawScope.drawChip(
    team: Team,
    center: Offset,
    radius: Float,
    alpha: Float = 1f,
    shadow: Boolean = true
) {
    val base = team.primaryColor()

    // Contact shadow, then the white rim.
    if (shadow) drawCircle(Color.Black, radius, center + Offset(0f, radius * 0.1f), alpha = 0.32f * alpha)
    drawCircle(Color.White, radius, center, alpha = alpha)

    // Edge spots on the rim.
    val spotRadius = radius * 0.89f
    repeat(6) { index ->
        drawArc(
            color = base,
            startAngle = index * 60f - 12f,
            sweepAngle = 24f,
            useCenter = false,
            topLeft = center - Offset(spotRadius, spotRadius),
            size = Size(spotRadius * 2, spotRadius * 2),
            alpha = alpha,
            style = Stroke(width = radius * 0.2f)
        )
    }

    // Glossy body lit from the top-left, with a thin inlay ring.
    val bodyRadius = radius * 0.76f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(team.chipHighlight(), base, team.chipShade()),
            center = center + Offset(-bodyRadius * 0.35f, -bodyRadius * 0.4f),
            radius = bodyRadius * 1.6f
        ),
        radius = bodyRadius,
        center = center,
        alpha = alpha
    )
    drawCircle(
        color = Color.White,
        radius = bodyRadius * 0.68f,
        center = center,
        alpha = 0.55f * alpha,
        style = Stroke(width = (radius * 0.07f).coerceAtLeast(1f))
    )
}

/** Share of the shatter spent cracking before the chip breaks apart. */
internal const val ShatterCrackPhase = 0.3f

/** Electric glow inside the cracks, matching the lightning. */
private val CrackGlow = Color(0xFFBDE8FF)

/**
 * A chip struck by lightning over [progress] (0..1): cracks race from the centre
 * to the rim while it trembles, then it breaks into six wedges that fly apart,
 * spin and fall, leaving a fading scorch mark. [seed] keeps every frame identical.
 */
internal fun DrawScope.drawShatteringChip(
    team: Team,
    center: Offset,
    radius: Float,
    progress: Float,
    seed: Int
) {
    if (progress >= 1f) return
    val random = Random(seed)
    val cuts = List(6) { i -> i * 60f + random.nextFloat() * 26f - 13f }
    val shards = List(6) { Pair(0.8f + random.nextFloat() * 0.45f, random.nextFloat() * 2f - 1f) }

    if (progress < ShatterCrackPhase) {
        // Cracks race out while the chip trembles harder and harder.
        val grow = progress / ShatterCrackPhase
        val tremble = Offset(sin(progress * 240f) * radius * 0.06f * (0.3f + 0.7f * grow), 0f)
        drawChip(team, center + tremble, radius)
        cuts.forEachIndexed { i, angle ->
            val crack = crackPath(center + tremble, radius, angle, grow, Random(seed * 7 + i))
            fun stroke(width: Float) = Stroke(width = radius * width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(crack, CrackGlow, alpha = 0.35f, style = stroke(0.2f))
            drawPath(crack, Color(0xFF111111), alpha = 0.9f, style = stroke(0.09f))
            drawPath(crack, CrackGlow, alpha = 0.95f, style = stroke(0.035f))
        }
        return
    }

    val t = (progress - ShatterCrackPhase) / (1f - ShatterCrackPhase)
    val eased = FastOutSlowInEasing.transform(t)
    val fade = 1f - ((t - 0.45f) / 0.55f).coerceIn(0f, 1f)

    // Scorch where the chip stood.
    drawCircle(Color.Black, radius * 0.95f, center, alpha = 0.3f * (1f - eased))

    cuts.forEachIndexed { i, start ->
        val end = if (i == cuts.lastIndex) cuts[0] + 360f else cuts[i + 1]
        val mid = (start + end) / 2f * (PI.toFloat() / 180f)
        val dir = Offset(cos(mid), sin(mid))
        val (speed, spin) = shards[i]
        val fly = dir * (radius * (0.15f + 1.5f * eased * speed))
        val fall = Offset(0f, radius * 1.6f * t * t)
        val wedge = Path().apply {
            moveTo(center.x, center.y)
            arcTo(Rect(center, radius * 1.01f), start, end - start, false)
            close()
        }
        withTransform({
            translate(fly.x + fall.x, fly.y + fall.y)
            rotate(spin * 150f * eased, pivot = center + dir * (radius * 0.55f))
        }) {
            clipPath(wedge) { drawChip(team, center, radius, alpha = fade, shadow = false) }
            drawPath(wedge, Color.Black, alpha = 0.35f * fade, style = Stroke(width = radius * 0.045f))
        }
    }
}

/** A jagged crack from near the centre towards the rim along [angle] (degrees). */
private fun crackPath(center: Offset, radius: Float, angle: Float, grow: Float, random: Random): Path {
    val path = Path()
    val steps = 4
    for (step in 0..steps) {
        val f = 0.12f + (1f - 0.12f) * step / steps
        if (f > 0.12f + (1f - 0.12f) * grow) break
        val a = (angle + if (step == 0) 0f else random.nextFloat() * 16f - 8f) * (PI.toFloat() / 180f)
        val point = center + Offset(cos(a), sin(a)) * (radius * f)
        if (step == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    return path
}
