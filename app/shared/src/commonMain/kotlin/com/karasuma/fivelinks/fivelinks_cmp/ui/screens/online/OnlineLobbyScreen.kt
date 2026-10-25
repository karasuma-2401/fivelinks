package com.karasuma.fivelinks.fivelinks_cmp.ui.screens.online

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karasuma.fivelinks.fivelinks_cmp.PlatformBackHandler
import com.karasuma.fivelinks.fivelinks_cmp.online.ConnectionState
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineRoom
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineSession
import com.karasuma.fivelinks.fivelinks_cmp.protocol.FirstPlayer
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomSettings
import com.karasuma.fivelinks.fivelinks_cmp.protocol.RoomStatus
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIcon
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.LineIconView
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.components.SegmentedSelector
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BrandRed
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.PureWhite

/**
 * The client-side half of online 1v1. It intentionally only renders [OnlineSession]'s state:
 * rooms and games remain entirely authoritative on the server.
 */
@Composable
fun OnlineLobbyScreen(
    session: OnlineSession?,
    initialPlayerName: String,
    serverConfigurationError: String?,
    onConnect: (String) -> Unit,
    onEnterGame: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlatformBackHandler(onBack = onExit)
    var playerName by rememberSaveable { mutableStateOf(initialPlayerName) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (session == null) {
            ConnectPanel(
                playerName = playerName,
                onNameChange = { playerName = it },
                onConnect = { onConnect(playerName.trim()) },
                serverConfigurationError = serverConfigurationError,
                onExit = onExit,
            )
        } else {
            val state by session.state.collectAsState()
            LaunchedEffect(state.game) {
                if (state.game != null) onEnterGame()
            }
            ConnectedLobby(
                session = session,
                connection = state.connection,
                room = state.room,
                busy = state.busy,
                error = state.error ?: state.roomClosed?.let { "Phòng đã hết hạn vì chưa có ai vào." },
                onExit = onExit,
            )
        }
    }
}

@Composable
private fun ConnectPanel(
    playerName: String,
    onNameChange: (String) -> Unit,
    onConnect: () -> Unit,
    serverConfigurationError: String?,
    onExit: () -> Unit,
) {
    LobbyPage {
        LobbyTitle("CHƠI ONLINE", "TẠO PHÒNG · NHẬP MÃ · ĐẤU 1V1")
        Spacer(Modifier.height(28.dp))
        LobbyCard {
            SectionLabel("TÊN HIỂN THỊ")
            OutlinedTextField(
                value = playerName,
                onValueChange = { onNameChange(it.take(20)) },
                singleLine = true,
                label = { Text("Tên của bạn") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Bạn sẽ được kết nối tới máy chủ trước khi tạo hoặc vào phòng.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            serverConfigurationError?.let {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(it)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "KẾT NỐI",
                enabled = playerName.trim().isNotEmpty() && serverConfigurationError == null,
                onClick = onConnect,
            )
        }
        MenuExit(onExit)
    }
}

@Composable
private fun ConnectedLobby(
    session: OnlineSession,
    connection: ConnectionState,
    room: OnlineRoom?,
    busy: Boolean,
    error: String?,
    onExit: () -> Unit,
) {
    LobbyPage {
        LobbyTitle("PHÒNG ONLINE", "ĐẤU 1V1 THEO LUẬT FIVE LINKS")
        Spacer(Modifier.height(16.dp))
        ConnectionBanner(connection)
        error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(it)
        }
        Spacer(Modifier.height(16.dp))

        when {
            room != null -> RoomPanel(room, busy, onCancelRoom = { session.leaveRoom() })
            connection is ConnectionState.Connected -> RoomPicker(session, busy)
            else -> ReconnectingPanel(connection)
        }

        MenuExit(onExit)
    }
}

@Composable
private fun RoomPicker(session: OnlineSession, busy: Boolean) {
    var sequenceToWin by rememberSaveable { mutableStateOf(2) }
    var tactical by rememberSaveable { mutableStateOf(true) }
    var firstPlayerIndex by rememberSaveable { mutableStateOf(0) }
    var roomCode by rememberSaveable { mutableStateOf("") }
    val firstPlayerChoices = listOf(FirstPlayer.RANDOM, FirstPlayer.HOST, FirstPlayer.GUEST)

    val settings = RoomSettings(
        sequenceToWin = sequenceToWin,
        tacticalCrafting = tactical,
        firstPlayer = firstPlayerChoices[firstPlayerIndex],
    )

    LobbyCard {
        SectionLabel("TẠO PHÒNG")
        Text(
            "Chọn luật cho trận đấu. Mã phòng sẽ hiện ngay sau khi tạo.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        SectionLabel("MỤC TIÊU CHIẾN THẮNG")
        SegmentedSelector(
            options = listOf("1 hàng · Nhanh", "2 hàng · Chuẩn"),
            selectedIndex = sequenceToWin - 1,
            onSelect = { sequenceToWin = it + 1 },
        )
        Spacer(Modifier.height(14.dp))
        SectionLabel("NGƯỜI ĐI ĐẦU")
        SegmentedSelector(
            options = listOf("Ngẫu nhiên", "Chủ phòng", "Khách"),
            selectedIndex = firstPlayerIndex,
            onSelect = { firstPlayerIndex = it },
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Switch) { tactical = !tactical }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Chế độ chiến thuật", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Ghép bài và Thiên Phạt Hoàng Kim",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = tactical,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = BrandRed, checkedThumbColor = PureWhite),
            )
        }
        PrimaryButton(
            text = "TẠO PHÒNG",
            enabled = !busy,
            onClick = { session.createRoom(settings) },
        )
    }

    Spacer(Modifier.height(14.dp))

    LobbyCard {
        SectionLabel("VÀO PHÒNG BẰNG MÃ")
        OutlinedTextField(
            value = roomCode,
            onValueChange = { roomCode = it.uppercase().take(12) },
            singleLine = true,
            label = { Text("Ví dụ: K7QP2M") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "VÀO PHÒNG",
            enabled = roomCode.isNotBlank() && !busy,
            onClick = { session.joinRoom(roomCode) },
        )
    }
}

@Composable
private fun RoomPanel(room: OnlineRoom, busy: Boolean, onCancelRoom: () -> Unit) {
    LobbyCard {
        when (room.status) {
            RoomStatus.WAITING -> {
                SectionLabel("ĐANG CHỜ ĐỐI THỦ")
                Text(
                    text = room.code,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 5.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = BrandRed,
                )
                Text(
                    "Gửi mã này cho bạn bè để họ vào trận.",
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoomStatus.PLAYING -> {
                SectionLabel("ĐANG VÀO TRẬN")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Đang đồng bộ bàn cờ…", fontWeight = FontWeight.Bold)
                }
            }
            RoomStatus.FINISHED -> {
                SectionLabel("TRẬN ĐẤU ĐÃ KẾT THÚC")
                Text(
                    "Đang mở kết quả trận đấu…",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        SectionLabel("NGƯỜI CHƠI")
        room.players.forEach { player ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(player.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        player.wantsRematch -> "SẴN SÀNG"
                        player.connected -> "ĐÃ KẾT NỐI"
                        else -> "MẤT KẾT NỐI"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (player.connected) MaterialTheme.colorScheme.primary else BrandRed,
                )
            }
        }

        if (room.status == RoomStatus.WAITING) {
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onCancelRoom,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("HỦY PHÒNG", fontWeight = FontWeight.Bold, color = BrandRed)
            }
        }
    }
}

@Composable
private fun ReconnectingPanel(connection: ConnectionState) {
    LobbyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("ĐANG KẾT NỐI", fontWeight = FontWeight.Black)
                Text(
                    connectionDetail(connection),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConnectionBanner(connection: ConnectionState) {
    val (label, detail, color) = when (connection) {
        ConnectionState.Offline -> Triple("CHƯA KẾT NỐI", "Đang chuẩn bị kết nối…", MaterialTheme.colorScheme.onSurfaceVariant)
        ConnectionState.Connecting -> Triple("ĐANG KẾT NỐI", "Đang liên hệ máy chủ…", MaterialTheme.colorScheme.primary)
        is ConnectionState.Connected -> Triple("ĐÃ KẾT NỐI", "Máy chủ đã sẵn sàng", MaterialTheme.colorScheme.primary)
        is ConnectionState.Reconnecting -> Triple("ĐANG KẾT NỐI LẠI", connectionDetail(connection), BrandRed)
        ConnectionState.UpdateRequired -> Triple("CẦN CẬP NHẬT", "Phiên bản game không tương thích với máy chủ.", BrandRed)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineIconView(LineIcon.Chip, color, Modifier.height(20.dp).width(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = color)
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrandRed.copy(alpha = 0.10f))
            .padding(12.dp),
        color = BrandRed,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )
}

@Composable
private fun LobbyPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun LobbyTitle(title: String, subtitle: String) {
    Text(
        title,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        subtitle,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LobbyCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = BrandRed, contentColor = PureWhite),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun MenuExit(onExit: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    TextButton(
        onClick = onExit,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Về menu chính" },
    ) {
        Text("VỀ MENU", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    }
}

private fun connectionDetail(connection: ConnectionState): String = when (connection) {
    is ConnectionState.Reconnecting -> "Thử lại sau ${connection.retryInMs / 1_000} giây…"
    ConnectionState.UpdateRequired -> "Hãy cập nhật FiveLink rồi thử lại."
    ConnectionState.Offline -> "Đang chuẩn bị kết nối…"
    ConnectionState.Connecting -> "Đang liên hệ máy chủ…"
    is ConnectionState.Connected -> "Đã kết nối"
}
