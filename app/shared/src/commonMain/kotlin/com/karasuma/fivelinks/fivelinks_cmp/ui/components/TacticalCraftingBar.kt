package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.TacticalAction

private val MiniCardWidth = 26.dp

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

@Composable
private fun TacticalCard(
    action: TacticalAction,
    onTriggerDivineWipe: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val isNight = FiveLinksTheme.colors.isNight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Extra room below: a selected hand card lifts into this space.
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp)
            .shadow(if (isNight) 0.dp else 10.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isNight) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The "recipe": the selected cards and, for a combo, the jack it becomes.
            when (action) {
                is TacticalAction.PairWild -> CraftRecipe(listOf(action.card1, action.card2), LineIcon.Sparkle)
                is TacticalAction.ConnectorSnipe -> CraftRecipe(listOf(action.card1, action.card2), LineIcon.Crosshair)
                is TacticalAction.DivineWipe -> CardStack(action.cards.sortedBy { it.rank.value }, step = 20.dp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            val (title, subtitle) = when (action) {
                is TacticalAction.PairWild -> "Ghép đôi" to "Chạm ô trống để đặt quân"
                is TacticalAction.ConnectorSnipe -> "Đồng chất liền kề" to "Chạm quân đối thủ để bắn tỉa"
                is TacticalAction.DivineWipe -> "Thiên Phạt Hoàng Kim" to "Xóa sạch toàn bộ quân đối thủ"
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // The legendary move gets a full-width trigger.
        if (action is TacticalAction.DivineWipe) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onTriggerDivineWipe,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BrandDark),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                LineIconView(icon = LineIcon.Bolt, color = BrandDark, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KÍCH HOẠT THIÊN PHẠT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

/** Two cards → the artificial jack they craft. */
@Composable
private fun CraftRecipe(cards: List<Card>, jackIcon: LineIcon) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Side by side so both ranks stay readable.
        CardStack(cards, step = MiniCardWidth + 3.dp)
        LineIconView(
            icon = LineIcon.ArrowRight,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        CraftedJack(icon = jackIcon)
    }
}

@Composable
private fun CardStack(cards: List<Card>, step: Dp) {
    val cardShape = RoundedCornerShape(MiniCardWidth * 0.22f)
    Box(
        modifier = Modifier
            .width(MiniCardWidth + step * (cards.size - 1))
            .height(MiniCardWidth * 1.25f)
    ) {
        cards.forEachIndexed { index, card ->
            MiniCard(
                card = card,
                width = MiniCardWidth,
                modifier = Modifier
                    .offset(x = step * index)
                    .shadow(if (index > 0 && step < MiniCardWidth) 3.dp else 0.dp, cardShape)
            )
        }
    }
}

/** A jack face with its role icon: sparkle (place anywhere) or crosshair (snipe). */
@Composable
private fun CraftedJack(icon: LineIcon) {
    val rankSize = with(LocalDensity.current) { (MiniCardWidth * 0.4f).toSp() }
    val shape = RoundedCornerShape(MiniCardWidth * 0.22f)
    val edge = FiveLinksTheme.colors.cardEdge
    Column(
        modifier = Modifier
            .size(MiniCardWidth, MiniCardWidth * 1.25f)
            .clip(shape)
            .background(BrandDark)
            .then(if (edge.alpha > 0f) Modifier.border(1.dp, edge, shape) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "J",
            fontSize = rankSize,
            lineHeight = rankSize,
            fontWeight = FontWeight.Bold,
            color = PureWhite
        )
        Spacer(modifier = Modifier.height(1.dp))
        LineIconView(icon = icon, color = PureWhite, modifier = Modifier.size(MiniCardWidth * 0.36f), strokeWidth = 2.6f)
    }
}
