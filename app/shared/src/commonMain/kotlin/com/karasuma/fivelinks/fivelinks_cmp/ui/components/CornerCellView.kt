package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent

/** Five-point star for the free corner cells (they count for every team). */
@Composable
fun CornerStarVector(
    modifier: Modifier = Modifier,
    color: Color = GoldAccent
) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 24f
        val offsetX = (size.width - 24f * scale) / 2f
        val offsetY = (size.height - 24f * scale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, Offset.Zero)
        }) {
            val path = Path().apply {
                val cx = 12f
                val cy = 12.6f
                for (i in 0 until 10) {
                    val angle = (-kotlin.math.PI / 2.0) + (i * kotlin.math.PI / 5.0)
                    val r = if (i % 2 == 0) 10.5f else 4.4f
                    val px = (cx + r * kotlin.math.cos(angle)).toFloat()
                    val py = (cy + r * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path, color)
        }
    }
}
