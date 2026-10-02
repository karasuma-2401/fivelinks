package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BoardCellBg
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.CornerGold

@Composable
fun CornerStarVector(
    modifier: Modifier = Modifier,
    color: Color = CornerGold
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
                val cy = 12f
                for (i in 0 until 10) {
                    val angle = (-kotlin.math.PI / 2.0) + (i * kotlin.math.PI / 5.0)
                    val r = if (i % 2 == 0) 10f else 4.2f
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

@Composable
fun CornerCellView(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(BoardCellBg),
        contentAlignment = Alignment.Center
    ) {
        CornerStarVector(
            modifier = Modifier.size(13.dp),
            color = CornerGold
        )
    }
}
