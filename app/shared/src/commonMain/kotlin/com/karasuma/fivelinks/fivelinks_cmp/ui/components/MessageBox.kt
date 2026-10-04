package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme

private val AlertBlueLight = Color(0xFF007AFF)
private val AlertBlueNight = Color(0xFF0A84FF)

/**
 * iOS-style alert: centred title and message above two side-by-side text
 * actions split by hairlines. The confirm action is red when it throws away
 * progress, like an iOS destructive action. Back press and taps outside cancel.
 */
@Composable
fun MessageBox(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Hủy",
    destructive: Boolean = true
) {
    val isNight = FiveLinksTheme.colors.isNight
    val container = if (isNight) Color(0xFF2C2C2E) else Color(0xFFF2F2F2)
    val textColor = if (isNight) Color.White else Color.Black
    val hairline = if (isNight) Color(0x99545458) else Color(0x4A3C3C43)
    val blue = if (isNight) AlertBlueNight else AlertBlueLight

    Dialog(onDismissRequest = onDismiss) {
        // Like iOS, the alert fades in while settling from a slightly larger size.
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            appear.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        }

        Column(
            modifier = Modifier
                .width(270.dp)
                .graphicsLayer {
                    val t = appear.value
                    alpha = t
                    scaleX = 1.12f - 0.12f * t
                    scaleY = 1.12f - 0.12f * t
                }
                .clip(RoundedCornerShape(14.dp))
                .background(container)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 19.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(hairline)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                AlertAction(text = dismissText, color = blue, onClick = onDismiss, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(hairline)
                )
                AlertAction(
                    text = confirmText,
                    color = if (destructive) BrandRed else blue,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Plain text action; it greys out while pressed, as on iOS. */
@Composable
private fun AlertAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressedTint = if (FiveLinksTheme.colors.isNight) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (pressed) pressedTint else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1
        )
    }
}
