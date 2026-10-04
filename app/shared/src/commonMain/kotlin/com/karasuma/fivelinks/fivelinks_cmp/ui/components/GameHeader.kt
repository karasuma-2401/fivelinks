package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.primaryColor

@Composable
fun GameHeader(
    gameState: GameState,
    isAiThinking: Boolean,
    elapsedSeconds: Int,
    onRestartClick: () -> Unit,
    onRulesClick: () -> Unit = {},
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer
    val humanCount = gameState.players.count { !it.isAi }
    val title = when {
        currentPlayer.isAi -> if (isAiThinking) "AI THINKING" else "AI TURN"
        humanCount == 1 -> "YOUR TURN"
        else -> "${currentPlayer.team.label()} TURN"
    }
    val teams = gameState.players.map { it.team }.distinct()

    fun teamLabel(team: Team): String {
        val members = gameState.players.filter { it.team == team }
        return when {
            members.any { it.isAi } -> "AI"
            humanCount == 1 -> "YOU"
            else -> team.label()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurnDot(color = currentPlayer.team.primaryColor(), pulsing = isAiThinking)
            Spacer(modifier = Modifier.width(10.dp))
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }) togetherWith
                        (fadeOut() + slideOutVertically { -it / 2 })
                },
                modifier = Modifier.weight(1f)
            ) { text ->
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HeaderIconButton(LineIcon.Help, "Luật chơi", onRulesClick)
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(LineIcon.Restart, "Chơi lại", onRestartClick)
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(LineIcon.Menu, "Về menu", onMenuClick)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scoreboard in the reference's stats style: big figures, tiny spaced labels.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            teams.forEach { team ->
                ScoreStat(
                    count = gameState.sequencesOf(team),
                    target = gameState.config.sequenceToWin,
                    label = teamLabel(team),
                    color = team.primaryColor()
                )
            }
            Stat(value = "${gameState.deck.size}", label = "DECK")
            Stat(value = "${gameState.turnNumber + 1}", label = "TURN")
            Stat(value = formatElapsed(elapsedSeconds), label = "TIME")
        }
    }
}

fun Team.label(): String = when (this) {
    Team.BLUE -> "BLUE"
    Team.RED -> "RED"
    Team.GREEN -> "GREEN"
}

/** mm:ss, or h:mm:ss past the hour. */
fun formatElapsed(totalSeconds: Int): String {
    fun two(value: Int) = value.toString().padStart(2, '0')
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "$hours:${two(minutes)}:${two(seconds)}" else "${two(minutes)}:${two(seconds)}"
}

/** Tabular figures keep the timer from jittering as digits change. */
private val StatValueStyle = TextStyle(
    fontSize = 22.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.ExtraBold,
    fontFeatureSettings = "tnum"
)

@Composable
private fun StatLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = LocalTextStyle.current.merge(StatValueStyle),
            color = MaterialTheme.colorScheme.onBackground
        )
        StatLabel(label)
    }
}

@Composable
private fun ScoreStat(count: Int, target: Int, label: String, color: Color) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = color)) { append("$count") }
                withStyle(SpanStyle(color = muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)) {
                    append("/$target")
                }
            },
            style = LocalTextStyle.current.merge(StatValueStyle)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            StatLabel(label)
        }
    }
}

@Composable
private fun TurnDot(color: Color, pulsing: Boolean) {
    val alpha = if (pulsing) {
        val pulse by rememberInfiniteTransition().animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        pulse
    } else {
        1f
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun HeaderIconButton(
    icon: LineIcon,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        LineIconView(
            icon = icon,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}
