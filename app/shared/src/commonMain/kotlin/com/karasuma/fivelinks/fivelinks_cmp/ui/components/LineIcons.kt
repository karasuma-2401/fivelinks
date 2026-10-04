package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class LineIcon {
    ArrowRight,
    Restart,
    Menu,
    Help,
    Moon,
    Bolt,
    Close
}

/**
 * Thin, geometric glyphs drawn on a 24×24 grid (like the "=", "+" and "→"
 * marks in the reference design). Drawn with Canvas so they need no icon
 * library and stay crisp on every platform.
 */
@Composable
fun LineIconView(
    icon: LineIcon,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        translate((size.width - 24f * unit) / 2f, (size.height - 24f * unit) / 2f) {
            val stroke = Stroke(width = strokeWidth * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
            when (icon) {
                LineIcon.ArrowRight -> {
                    drawLine(color, p(4f, 12f), p(19.5f, 12f), stroke.width, StrokeCap.Round)
                    drawPath(polyline(unit, 13.5f to 6f, 19.5f to 12f, 13.5f to 18f), color, style = stroke)
                }
                LineIcon.Restart -> drawRestart(color, unit, stroke)
                LineIcon.Menu -> {
                    drawLine(color, p(5f, 9f), p(19f, 9f), stroke.width, StrokeCap.Round)
                    drawLine(color, p(5f, 15f), p(19f, 15f), stroke.width, StrokeCap.Round)
                }
                LineIcon.Help -> {
                    val hook = Path().apply {
                        moveTo(8.8f * unit, 9.2f * unit)
                        cubicTo(8.8f * unit, 4.9f * unit, 15.2f * unit, 4.9f * unit, 15.2f * unit, 9.2f * unit)
                        cubicTo(15.2f * unit, 11.6f * unit, 12f * unit, 11.9f * unit, 12f * unit, 14.4f * unit)
                    }
                    drawPath(hook, color, style = stroke)
                    drawCircle(color, radius = 1.35f * unit, center = p(12f, 18.6f))
                }
                LineIcon.Moon -> {
                    val disc = Path().apply { addOval(rectOf(unit, 12f, 12f, 8.5f)) }
                    val bite = Path().apply { addOval(rectOf(unit, 16.5f, 8f, 7.2f)) }
                    val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
                    drawPath(crescent, color)
                }
                LineIcon.Bolt -> {
                    drawPath(
                        polyline(
                            unit,
                            13.5f to 2.5f, 5f to 13.5f, 11.2f to 13.5f,
                            10.2f to 21.5f, 19f to 10.2f, 12.8f to 10.2f
                        ).apply { close() },
                        color
                    )
                }
                LineIcon.Close -> {
                    drawLine(color, p(6.5f, 6.5f), p(17.5f, 17.5f), stroke.width, StrokeCap.Round)
                    drawLine(color, p(17.5f, 6.5f), p(6.5f, 17.5f), stroke.width, StrokeCap.Round)
                }
            }
        }
    }
}

private fun polyline(unit: Float, vararg points: Pair<Float, Float>) = Path().apply {
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) moveTo(x * unit, y * unit) else lineTo(x * unit, y * unit)
    }
}

private fun rectOf(unit: Float, cx: Float, cy: Float, radius: Float) =
    Rect(
        offset = Offset((cx - radius) * unit, (cy - radius) * unit),
        size = Size(radius * 2 * unit, radius * 2 * unit)
    )

/** Clockwise circular arrow with the head pointing into the gap. */
private fun DrawScope.drawRestart(color: Color, unit: Float, stroke: Stroke) {
    val radius = 7.5f * unit
    val center = Offset(12f * unit, 12.5f * unit)
    val startAngle = -60f
    val sweep = 290f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = stroke
    )
    val end = (startAngle + sweep) * PI.toFloat() / 180f
    val tip = Offset(center.x + radius * cos(end), center.y + radius * sin(end))
    // Tangent of a clockwise sweep in screen coordinates, and its normal.
    val dir = Offset(-sin(end), cos(end))
    val normal = Offset(-dir.y, dir.x)
    val head = 3.6f * unit
    val arrow = Path().apply {
        moveTo(tip.x + dir.x * head, tip.y + dir.y * head)
        lineTo(tip.x - dir.x * head * 0.35f + normal.x * head, tip.y - dir.y * head * 0.35f + normal.y * head)
        lineTo(tip.x - dir.x * head * 0.35f - normal.x * head, tip.y - dir.y * head * 0.35f - normal.y * head)
        close()
    }
    drawPath(arrow, color)
}
