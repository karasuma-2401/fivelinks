package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.chipBorderColor
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.chipFillColor

@Composable
fun ChipView(
    team: Team,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    isLastMove: Boolean = false,
    size: Dp = 26.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val fillColor = team.chipFillColor()
    val borderColor = team.chipBorderColor()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = (this.size.minDimension / 2) * 0.90f

            // 1. Last Move Halo
            if (isLastMove) {
                drawCircle(
                    color = Color.White.copy(alpha = pulseAlpha),
                    radius = radius + 2.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 2. Chip Drop Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = radius,
                center = center + Offset(0f, 1.dp.toPx())
            )

            // 3. Translucent Frosted Acrylic Body
            drawCircle(
                color = fillColor,
                radius = radius,
                center = center
            )

            // 4. Crisp Outer Border
            drawCircle(
                color = if (isLocked) GoldAccent else borderColor,
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 5. Inner Golden Ring if Locked
            if (isLocked) {
                drawCircle(
                    color = GoldAccent,
                    radius = radius * 0.65f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
    }
}
