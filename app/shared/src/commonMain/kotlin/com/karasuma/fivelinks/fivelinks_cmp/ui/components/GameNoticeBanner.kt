package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import kotlinx.coroutines.delay

/** One line of match commentary, e.g. "AI đặt quân vào" + the card of that cell. */
@Immutable
data class GameNotice(
    val id: Long,
    val text: String,
    val accent: Color,
    val icon: LineIcon,
    val card: Card? = null,
    /** Big moments (Thiên Phạt) stay up a little longer before the next notice. */
    val emphasis: Boolean = false
)

/** Shows the latest [notice] for a few seconds; a newer notice replaces it in place. */
@Composable
fun GameNoticeBanner(
    notice: GameNotice?,
    modifier: Modifier = Modifier
) {
    var visible by remember(notice?.id) { mutableStateOf(notice != null) }
    LaunchedEffect(notice?.id) {
        if (notice != null) {
            delay(2800)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && notice != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = notice,
            contentKey = { it?.id },
            transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut() }
        ) { current ->
            if (current != null) {
                NoticePill(current)
            }
        }
    }
}

@Composable
private fun NoticePill(notice: GameNotice) {
    val isNight = FiveLinksTheme.colors.isNight
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .shadow(if (isNight) 0.dp else 12.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isNight) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier)
            .padding(start = 6.dp, end = if (notice.card != null) 6.dp else 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(notice.accent),
            contentAlignment = Alignment.Center
        ) {
            LineIconView(icon = notice.icon, color = PureWhite, modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = notice.text,
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (notice.card != null) {
            Spacer(modifier = Modifier.width(10.dp))
            MiniCard(card = notice.card)
        }
    }
}
