package com.karasuma.fivelinks.fivelinks_cmp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.karasuma.fivelinks.fivelinks_cmp.model.LeaderboardResponse
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerLeaderboardEntry
import com.karasuma.fivelinks.fivelinks_cmp.model.RankTier
import com.karasuma.fivelinks.fivelinks_cmp.online.LeaderboardClient
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.components.SegmentedSelector
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.GoldAccent
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun LeaderboardDialog(
    playerId: String,
    playerName: String,
    onDismiss: () -> Unit,
    leaderboardClient: LeaderboardClient = remember { LeaderboardClient() }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var response by remember { mutableStateOf<LeaderboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            leaderboardClient.getLeaderboard(playerId = playerId)
                .onSuccess {
                    response = it
                    isLoading = false
                }
                .onFailure {
                    errorMessage = "Không thể tải bảng xếp hạng trực tuyến. Đang hiển thị điểm ngoại tuyến."
                    isLoading = false
                }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val shape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = 700.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BẢNG VINH DANH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                color = GoldAccent
                            )
                            Text(
                                text = "Xếp Hạng Toàn Cầu",
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(role = Role.Button, onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            LineIconView(
                                icon = LineIcon.Close,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 2 Tabs
                    SegmentedSelector(
                        options = listOf("Top 50 Toàn Cầu", "Hồ Sơ Của Bạn"),
                        selectedIndex = selectedTab,
                        onSelect = { selectedTab = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.weight(1f)
                    ) { tab ->
                        when {
                            isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = GoldAccent,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                            tab == 0 -> {
                                GlobalLeaderboardTab(
                                    response = response,
                                    currentPlayerId = playerId,
                                    currentFallbackName = playerName
                                )
                            }
                            else -> {
                                PlayerProfileTab(
                                    entry = response?.currentPlayer ?: PlayerLeaderboardEntry(
                                        playerId = playerId,
                                        playerName = playerName,
                                        rating = 1000,
                                        tier = RankTier.BRONZE
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalLeaderboardTab(
    response: LeaderboardResponse?,
    currentPlayerId: String,
    currentFallbackName: String
) {
    val players = response?.topPlayers ?: emptyList()

    if (players.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có dữ liệu xếp hạng trực tuyến.\nHãy là người chơi đầu tiên ghi tên vào Bảng Vàng!",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(players) { index, player ->
                LeaderboardRow(
                    entry = player,
                    isCurrentPlayer = player.playerId == currentPlayerId
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: PlayerLeaderboardEntry,
    isCurrentPlayer: Boolean
) {
    val rank = entry.rank
    val medalColor = when (rank) {
        1 -> GoldAccent
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerBg = when {
        isCurrentPlayer -> BrandRed.copy(alpha = 0.08f)
        rank == 1 -> GoldAccent.copy(alpha = 0.07f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val borderStroke = when {
        isCurrentPlayer -> 1.5.dp to BrandRed
        rank == 1 -> 1.dp to GoldAccent
        else -> 1.dp to Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerBg,
        border = androidx.compose.foundation.BorderStroke(borderStroke.first, borderStroke.second),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number or Medal
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(medalColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "#$rank"
                    },
                    fontSize = if (rank <= 3) 15.sp else 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = medalColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.playerName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentPlayer) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "(BẠN)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandRed,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${entry.tier.displayName} • ${entry.wins} thắng (${entry.winRatePercent}%)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rating Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldAccent.copy(alpha = 0.15f),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${entry.rating} ĐIỂM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerProfileTab(
    entry: PlayerLeaderboardEntry
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tier Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(entry.tier.badgeColorHex).copy(alpha = 0.18f))
                .border(2.dp, Color(entry.tier.badgeColorHex), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            LineIconView(
                icon = LineIcon.Sparkle,
                color = Color(entry.tier.badgeColorHex),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = entry.tier.displayName.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color(entry.tier.badgeColorHex)
        )

        Text(
            text = "${entry.rating} MMR",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStatCard(
                title = "TRẬN ĐẤU",
                value = "${entry.matchesPlayed}",
                subtitle = "${entry.wins} trận thắng",
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                title = "TỈ LỆ THẮNG",
                value = "${entry.winRatePercent}%",
                subtitle = "Chuỗi đỉnh cao",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStatCard(
                title = "CHUỖI SEQUENCE",
                value = "${entry.sequencesCompleted}",
                subtitle = "5 ô liên tiếp đã khóa",
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                title = "BẮN TỈA",
                value = "${entry.snipesCount}",
                subtitle = "Quân đối thủ đã gỡ",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Guest info notice
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LineIconView(LineIcon.Star, color = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tài khoản khách được đồng bộ tự động theo thiết bị qua Neon DB.",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

