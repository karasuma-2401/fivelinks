package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game.GameScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuSettings
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.online.OnlineLobbyScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameController
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineClient
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineGameController
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineSession
import kotlinx.coroutines.launch

enum class AppScreen {
    MENU,
    ONLINE_LOBBY,
    GAME
}

/**
 * @param onNightThemeChange lets the host match system bar icons to the in-app
 * theme (Android's status bar would otherwise follow the system theme).
 */
@Composable
fun App(onNightThemeChange: (Boolean) -> Unit = {}) {
    val systemDark = isSystemInDarkTheme()
    var isNightTheme by rememberSaveable { mutableStateOf(systemDark) }
    LaunchedEffect(isNightTheme) { onNightThemeChange(isNightTheme) }

    FiveLinksTheme(darkTheme = isNightTheme) {
        var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
        var activeController by remember { mutableStateOf<GameController?>(null) }
        var onlineSession by remember { mutableStateOf<OnlineSession?>(null) }
        var menuSettings by remember { mutableStateOf(MenuSettings()) }
        val scope = rememberCoroutineScope()

        fun exitOnline() {
            val leavingSession = onlineSession
            onlineSession = null
            activeController = null
            currentScreen = AppScreen.MENU
            scope.launch { leavingSession?.leaveRoomAndClose() }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 350)
            ) { screen ->
                when (screen) {
                    AppScreen.MENU -> {
                        MenuScreen(
                            onStartGame = { config, players, difficulty ->
                                activeController = GameViewModel(
                                    initialConfig = config,
                                    initialPlayers = players,
                                    aiDifficulty = difficulty
                                )
                                currentScreen = AppScreen.GAME
                            },
                            onStartOnline = { currentScreen = AppScreen.ONLINE_LOBBY },
                            settings = menuSettings,
                            onSettingsChange = { menuSettings = it },
                            isNightTheme = isNightTheme,
                            onToggleNightTheme = { isNightTheme = it }
                        )
                    }

                    AppScreen.ONLINE_LOBBY -> OnlineLobbyScreen(
                        session = onlineSession,
                        onConnect = { playerName ->
                            onlineSession?.close()
                            onlineSession = OnlineSession(OnlineClient(playerName, scope), scope).also { it.start() }
                        },
                        onEnterGame = {
                            onlineSession?.let { session ->
                                activeController = OnlineGameController(session, scope)
                                currentScreen = AppScreen.GAME
                            }
                        },
                        onExit = ::exitOnline,
                    )

                    AppScreen.GAME -> {
                        val controller = activeController
                        if (controller != null) {
                            GameScreen(
                                controller = controller,
                                onExitToMenu = {
                                    if (controller is OnlineGameController) exitOnline()
                                    else {
                                        activeController = null
                                        currentScreen = AppScreen.MENU
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
