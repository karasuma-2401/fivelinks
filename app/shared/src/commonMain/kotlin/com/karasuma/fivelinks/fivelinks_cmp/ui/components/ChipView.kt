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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.glowColor
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.lightColor
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

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
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val primary = team.primaryColor()
    val light = team.lightColor()
    val glow = team.glowColor()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = (this.size.minDimension / 2) * 0.92f

            // 1. Last Move Halo
            if (isLastMove) {
                drawCircle(
                    color = Color.White.copy(alpha = pulseAlpha),
                    radius = radius + 2.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 2. Chip Drop Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = radius,
                center = center + Offset(0f, 1.5.dp.toPx())
            )

            // 3. Chip Main Body (Radial 3D Gradient)
            val chipGradient = Brush.radialGradient(
                colors = listOf(light, primary, primary.copy(alpha = 0.85f)),
                center = center - Offset(radius * 0.25f, radius * 0.25f),
                radius = radius * 1.2f
            )
            drawCircle(
                brush = chipGradient,
                radius = radius,
                center = center
            )

            // 4. Concentric Inner Ring
            val ringColor = if (isLocked) GoldAccent else Color.White.copy(alpha = 0.4f)
            val ringWidth = if (isLocked) 1.5.dp.toPx() else 0.8.dp.toPx()
            drawCircle(
                color = ringColor,
                radius = radius * 0.65f,
                center = center,
                style = Stroke(width = ringWidth)
            )

            // 5. Outer Golden Edge if Locked
            if (isLocked) {
                drawCircle(
                    color = GoldAccent,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Lock Icon if Locked
        if (isLocked) {
            Text(
                text = "🔒",
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}
