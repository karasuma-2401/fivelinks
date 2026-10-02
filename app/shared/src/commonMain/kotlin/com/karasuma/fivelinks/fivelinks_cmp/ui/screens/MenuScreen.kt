package com.karasuma.fivelinks.fivelinks_cmp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.RulesDialog
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BackgroundDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.SurfaceElevated
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlue
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamBlueGlow
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.TeamRed

enum class OpponentType {
    AI,
    LOCAL_PASS_AND_PLAY
}

@Composable
fun MenuScreen(
    onStartGame: (GameConfig, List<Player>, Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    var opponentType by remember { mutableStateOf(OpponentType.AI) }
    var aiDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var enableTactical by remember { mutableStateOf(true) }
    var showRules by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title & Logo
            Text(
                text = "FIVELINKS",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = GoldAccent
            )
            Text(
                text = "Cờ Nối 5 Lá • Sequence Chiến Thuật & AI",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Game Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CHẾ ĐỘ ĐỐI THỦ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeSelectButton(
                            text = "🤖 Đấu Với AI",
                            isSelected = opponentType == OpponentType.AI,
                            onClick = { opponentType = OpponentType.AI },
                            modifier = Modifier.weight(1f)
                        )
                        ModeSelectButton(
                            text = "👥 2 Người (Local)",
                            isSelected = opponentType == OpponentType.LOCAL_PASS_AND_PLAY,
                            onClick = { opponentType = OpponentType.LOCAL_PASS_AND_PLAY },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // AI Difficulty Options if playing against AI
                    if (opponentType == OpponentType.AI) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "ĐỘ KHÓ AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Difficulty.entries.forEach { diff ->
                                val label = when (diff) {
                                    Difficulty.EASY -> "Dễ"
                                    Difficulty.MEDIUM -> "Vừa"
                                    Difficulty.HARD -> "Khó"
                                }
                                DifficultyButton(
                                    text = label,
                                    isSelected = aiDifficulty == diff,
                                    onClick = { aiDifficulty = diff },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rules & Tactical Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Chế Độ Chiến Thuật (Tactical)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ghép đôi (Wild), Suited connector (Bắn tỉa), Thiên Phạt",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = enableTactical,
                            onCheckedChange = { enableTactical = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = GoldAccent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Start Game Button
            Button(
                onClick = {
                    val config = if (enableTactical) {
                        GameConfig.tactical(playerCount = 2, teams = 2)
                    } else {
                        GameConfig.soloVsAi()
                    }
                    val players = if (opponentType == OpponentType.AI) {
                        listOf(
                            Player(id = "p0", name = "Người chơi", team = Team.BLUE, isAi = false),
                            Player(id = "p1", name = "AI Bot (${aiDifficulty.name})", team = Team.RED, isAi = true)
                        )
                    } else {
                        listOf(
                            Player(id = "p0", name = "Người chơi 1 (Xanh)", team = Team.BLUE, isAi = false),
                            Player(id = "p1", name = "Người chơi 2 (Đỏ)", team = Team.RED, isAi = false)
                        )
                    }
                    onStartGame(config, players, aiDifficulty)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(52.dp)
            ) {
                Text(
                    text = "VÀO VÁN ĐẤU 🎮",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // View Rules Button
            OutlinedButton(
                onClick = { showRules = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(44.dp)
            ) {
                Text(
                    text = "📖 Xem Hướng Dẫn Luật Chơi",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        if (showRules) {
            RulesDialog(onDismiss = { showRules = false })
        }
    }
}

@Composable
private fun ModeSelectButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) TeamBlue else SurfaceElevated)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) TeamBlueGlow else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DifficultyButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) GoldAccent else SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else Color(0xFFCBD5E1)
        )
    }
}
