package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.TacticalAction

@Composable
fun TacticalCraftingBar(
    tacticalAction: TacticalAction?,
    onTriggerDivineWipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    // AnimatedContent keeps rendering the outgoing action while it collapses.
    AnimatedContent(
        targetState = tacticalAction,
        contentKey = { action -> action?.let { it::class } },
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(150)) using SizeTransform(clip = false)
        },
        modifier = modifier.fillMaxWidth()
    ) { action ->
        if (action == null) {
            Spacer(modifier = Modifier.fillMaxWidth())
        } else {
            TacticalCard(action = action, onTriggerDivineWipe = onTriggerDivineWipe)
        }
    }
}

private data class TacticalCopy(
    val accent: Color,
    val title: String,
    val subtitle: String,
    val tag: String?
)

@Composable
private fun TacticalCard(
    action: TacticalAction,
    onTriggerDivineWipe: () -> Unit
) {
    val copy = when (action) {
        is TacticalAction.PairWild -> TacticalCopy(
            accent = TeamBlue,
            title = "Ghép đôi · Jack 2 mắt",
            subtitle = "Chạm vào một ô trống bất kỳ để đặt quân",
            tag = "WILD"
        )
        is TacticalAction.ConnectorSnipe -> TacticalCopy(
            accent = BrandRed,
            title = "Đồng chất liền kề · Jack 1 mắt",
            subtitle = "Chạm vào một quân đối thủ chưa khóa để bắn tỉa",
            tag = "SNIPE"
        )
        is TacticalAction.DivineWipe -> TacticalCopy(
            accent = GoldAccent,
            title = "Thiên Phạt Hoàng Kim",
            subtitle = "Sảnh đồng chất 5 lá: xóa sạch quân đối thủ",
            tag = null
        )
    }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(if (FiveLinksTheme.colors.isNight) 0.dp else 10.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, copy.accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(copy.accent),
            contentAlignment = Alignment.Center
        ) {
            LineIconView(icon = LineIcon.Bolt, color = PureWhite, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = copy.title,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = copy.subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (copy.tag != null) {
            Text(
                text = copy.tag,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = copy.accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(copy.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        } else {
            Button(
                onClick = onTriggerDivineWipe,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BrandDark),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "KÍCH HOẠT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
