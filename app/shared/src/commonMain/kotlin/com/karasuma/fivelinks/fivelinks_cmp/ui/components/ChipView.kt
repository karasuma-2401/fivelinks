package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.chipHighlight
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.chipShade
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

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
    val base = team.primaryColor()
    val highlight = team.chipHighlight()
    val shade = team.chipShade()

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

        // Contact shadow, then the white rim.
        drawCircle(Color.Black.copy(alpha = 0.32f), radius, center + Offset(0f, radius * 0.1f))
        drawCircle(Color.White, radius, center)

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
                style = Stroke(width = radius * 0.2f)
            )
        }

        // Glossy body lit from the top-left, with a thin inlay ring.
        val bodyRadius = radius * 0.76f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(highlight, base, shade),
                center = center + Offset(-bodyRadius * 0.35f, -bodyRadius * 0.4f),
                radius = bodyRadius * 1.6f
            ),
            radius = bodyRadius,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = bodyRadius * 0.68f,
            center = center,
            style = Stroke(width = (radius * 0.07f).coerceAtLeast(1f))
        )
    }
}
