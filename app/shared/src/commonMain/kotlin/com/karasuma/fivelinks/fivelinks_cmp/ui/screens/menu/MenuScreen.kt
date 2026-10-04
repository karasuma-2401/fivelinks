package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Rank
import com.karasuma.fivelinks.fivelinks_cmp.domain.Suit
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIcon
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIconView
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.PlayingCardFace
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.RulesDialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.components.SegmentedSelector
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import kotlin.math.abs
import kotlin.random.Random

enum class OpponentType {
    AI,
    LOCAL_PASS_AND_PLAY
}

/** Lobby choices, hoisted so they survive a round trip to the game screen. */
data class MenuSettings(
    val opponentType: OpponentType = OpponentType.AI,
    val aiDifficulty: Difficulty = Difficulty.MEDIUM,
    val enableTactical: Boolean = true,
    val sequenceToWin: Int = 2
)

@Composable
fun MenuScreen(
    onStartGame: (GameConfig, List<Player>, Difficulty) -> Unit,
    modifier: Modifier = Modifier,
    settings: MenuSettings = MenuSettings(),
    onSettingsChange: (MenuSettings) -> Unit = {},
    isNightTheme: Boolean = false,
    onToggleNightTheme: (Boolean) -> Unit = {}
) {
    var showRules by remember { mutableStateOf(false) }
    val difficulties = Difficulty.entries

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            HeroCardFan()
            Spacer(modifier = Modifier.height(8.dp))
            Logo()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "SEQUENCE  ·  TACTICAL  ·  AI",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsPanel {
                SectionLabel("ĐỐI THỦ")
                SegmentedSelector(
                    options = listOf("Đấu với AI", "Chơi 2 người"),
                    selectedIndex = if (settings.opponentType == OpponentType.AI) 0 else 1,
                    onSelect = { index ->
                        onSettingsChange(
                            settings.copy(
                                opponentType = if (index == 0) OpponentType.AI else OpponentType.LOCAL_PASS_AND_PLAY
                            )
                        )
                    }
                )

                AnimatedVisibility(
                    visible = settings.opponentType == OpponentType.AI,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        SectionLabel("ĐỘ KHÓ AI")
                        SegmentedSelector(
                            options = difficulties.map { it.label() },
                            selectedIndex = difficulties.indexOf(settings.aiDifficulty),
                            onSelect = { index -> onSettingsChange(settings.copy(aiDifficulty = difficulties[index])) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                SectionLabel("MỤC TIÊU CHIẾN THẮNG")
                SegmentedSelector(
                    options = listOf("1 hàng · Nhanh", "2 hàng · Chuẩn"),
                    selectedIndex = settings.sequenceToWin - 1,
                    onSelect = { index -> onSettingsChange(settings.copy(sequenceToWin = index + 1)) }
                )

                Spacer(modifier = Modifier.height(10.dp))
                PanelDivider()
                SettingToggle(
                    icon = LineIcon.Bolt,
                    title = "Chế độ chiến thuật",
                    subtitle = "Ghép đôi (Wild) · Đồng chất liền kề (Snipe) · Thiên Phạt",
                    checked = settings.enableTactical,
                    onCheckedChange = { onSettingsChange(settings.copy(enableTactical = it)) }
                )
                PanelDivider()
                SettingToggle(
                    icon = LineIcon.Moon,
                    title = "Giao diện ban đêm",
                    subtitle = "Turn your lights down low",
                    checked = isNightTheme,
                    onCheckedChange = onToggleNightTheme
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { startGame(settings, onStartGame) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
                elevation = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, CircleShape, ambientColor = BrandRed, spotColor = BrandRed)
            ) {
                Text(
                    text = "BẮT ĐẦU TRẬN ĐẤU",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                LineIconView(icon = LineIcon.ArrowRight, color = PureWhite, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = { showRules = true },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                LineIconView(
                    icon = LineIcon.Help,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HƯỚNG DẪN LUẬT CHƠI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showRules) {
            RulesDialog(onDismiss = { showRules = false })
        }
    }
}

private fun startGame(
    settings: MenuSettings,
    onStartGame: (GameConfig, List<Player>, Difficulty) -> Unit
) {
    val baseConfig = if (settings.enableTactical) {
        GameConfig.tactical(playerCount = 2, teams = 2)
    } else {
        GameConfig.soloVsAi()
    }
    // A fresh seed per match; the default seed dealt the same cards every game.
    val config = baseConfig.copy(sequenceToWin = settings.sequenceToWin, seed = Random.nextLong())

    val players = if (settings.opponentType == OpponentType.AI) {
        listOf(
            Player(id = "p0", name = "Người chơi", team = Team.BLUE, isAi = false),
            Player(id = "p1", name = "AI (${settings.aiDifficulty.label()})", team = Team.RED, isAi = true)
        )
    } else {
        listOf(
            Player(id = "p0", name = "Người chơi 1 (Xanh)", team = Team.BLUE, isAi = false),
            Player(id = "p1", name = "Người chơi 2 (Đỏ)", team = Team.RED, isAi = false)
        )
    }
    onStartGame(config, players, settings.aiDifficulty)
}

private fun Difficulty.label(): String = when (this) {
    Difficulty.EASY -> "Dễ"
    Difficulty.MEDIUM -> "Vừa"
    Difficulty.HARD -> "Khó"
}

/** Five cards that fan open when the lobby appears. */
@Composable
private fun HeroCardFan(modifier: Modifier = Modifier) {
    val cards = remember {
        listOf(
            Card(Suit.HEARTS, Rank.TEN),
            Card(Suit.SPADES, Rank.JACK),
            Card(Suit.DIAMONDS, Rank.QUEEN),
            Card(Suit.CLUBS, Rank.KING),
            Card(Suit.HEARTS, Rank.ACE)
        )
    }
    val spread = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        spread.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
    }
    val cardWidth = 58.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(cardWidth * 1.5f + 28.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        cards.forEachIndexed { index, card ->
            val fromMiddle = index - (cards.size - 1) / 2f
            PlayingCardFace(
                card = card,
                width = cardWidth,
                showJackRole = false,
                modifier = Modifier.graphicsLayer {
                    val s = spread.value
                    translationX = fromMiddle * 34.dp.toPx() * s
                    translationY = abs(fromMiddle) * 6.dp.toPx() * s - 10.dp.toPx()
                    rotationZ = fromMiddle * 12f * s
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    shadowElevation = 10.dp.toPx()
                    shape = RoundedCornerShape(cardWidth * 0.14f)
                }
            )
        }
    }
}

@Composable
private fun Logo() {
    // The wordmark is sized in dp so system font scaling cannot push it off screen.
    val density = LocalDensity.current
    val size = with(density) { 38.dp.toSp() }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append("FIVE") }
            withStyle(SpanStyle(color = BrandRed)) { append("LINKS") }
        },
        fontSize = size,
        lineHeight = size * 1.15f,
        fontWeight = FontWeight.Black,
        letterSpacing = with(density) { 2.dp.toSp() }
    )
}

@Composable
private fun SettingsPanel(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val isNight = FiveLinksTheme.colors.isNight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNight) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = 20.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.14f)
                    )
                }
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (isNight) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun PanelDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingToggle(
    icon: LineIcon,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BrandRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            LineIconView(icon = icon, color = BrandRed, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = BrandRed,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
fun MenuScreenPreview() {
    FiveLinksTheme {
        MenuScreen(
            onStartGame = { _, _, _ -> }
        )
    }
}
