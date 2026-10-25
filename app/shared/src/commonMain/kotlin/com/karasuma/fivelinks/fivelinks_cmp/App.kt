package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.audio.LocalSoundEnabled
import com.karasuma.fivelinks.fivelinks_cmp.audio.LocalVibrationEnabled
import com.karasuma.fivelinks.fivelinks_cmp.online.InMemoryOnlineProfileStore
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineClient
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineGameController
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineProfile
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineProfileStore
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineSession
import com.karasuma.fivelinks.fivelinks_cmp.online.ServerConfig
import com.karasuma.fivelinks.fivelinks_cmp.storage.InMemoryOfflineStorage
import com.karasuma.fivelinks.fivelinks_cmp.storage.OfflineStorage
import com.karasuma.fivelinks.fivelinks_cmp.storage.SavedOfflineGame
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game.GameScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuSettings
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.online.OnlineLobbyScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.tutorial.TutorialScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameController
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

enum class AppScreen {
    MENU,
    ONLINE_LOBBY,
    GAME,
    TUTORIAL
}

/**
 * @param onNightThemeChange lets the host match system bar icons to the in-app
 * theme (Android's status bar would otherwise follow the system theme).
 */
@Composable
fun App(
    onNightThemeChange: (Boolean) -> Unit = {},
    onlineServerUrl: String = ServerConfig.DEV_URL,
    requireSecureOnline: Boolean = false,
    onlineProfileStore: OnlineProfileStore = InMemoryOnlineProfileStore,
    offlineStorage: OfflineStorage = remember { InMemoryOfflineStorage() },
) {
    val initialPrefs = remember { offlineStorage.loadPreferences() }
    val systemDark = isSystemInDarkTheme()
    var isNightTheme by rememberSaveable { mutableStateOf(initialPrefs.isNightTheme ?: systemDark) }
    LaunchedEffect(isNightTheme) {
        onNightThemeChange(isNightTheme)
        offlineStorage.savePreferences(offlineStorage.loadPreferences().copy(isNightTheme = isNightTheme))
    }

    var soundEnabled by remember { mutableStateOf(initialPrefs.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(initialPrefs.vibrationEnabled) }
    var hasCompletedOnboarding by remember { mutableStateOf(initialPrefs.hasCompletedOnboarding) }
    var savedGame by remember {
        mutableStateOf(offlineStorage.loadSavedGame()?.takeIf { !it.gameState.isGameOver })
    }

    var menuSettings by remember {
        mutableStateOf(initialPrefs.toMenuSettings())
    }

    FiveLinksTheme(darkTheme = isNightTheme) {
        CompositionLocalProvider(
            LocalSoundEnabled provides soundEnabled,
            LocalVibrationEnabled provides vibrationEnabled
        ) {
            var currentScreen by remember {
                mutableStateOf(if (!hasCompletedOnboarding) AppScreen.TUTORIAL else AppScreen.MENU)
            }
            var activeController by remember { mutableStateOf<GameController?>(null) }
            var onlineSession by remember { mutableStateOf<OnlineSession?>(null) }
            var onlineProfile by remember { mutableStateOf(onlineProfileStore.load()) }
            val scope = rememberCoroutineScope()
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val serverConfigurationError = remember(onlineServerUrl, requireSecureOnline) {
                ServerConfig.validationError(onlineServerUrl, requireSecureOnline)
            }

            // Keep a room resumable while the app is backgrounded. A process restart creates a new
            // client later with the persisted opaque token, so the server can restore the same room.
            DisposableEffect(lifecycle, onlineSession) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> onlineSession?.pause()
                        Lifecycle.Event.ON_START -> onlineSession?.resume()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            fun exitOnline() {
                val leavingSession = onlineSession
                onlineSession = null
                activeController?.close()
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
                            val savedSummary = savedGame?.let { saved ->
                                val mode = if (saved.gameState.config.enableTacticalCrafting) "Chiến thuật" else "Cổ điển"
                                val turn = saved.gameState.turnNumber
                                val diffName = when (saved.aiDifficulty) {
                                    Difficulty.EASY -> "Dễ"
                                    Difficulty.MEDIUM -> "Vừa"
                                    Difficulty.HARD -> "Khó"
                                }
                                "$mode · Lượt $turn · Độ khó: $diffName"
                            }

                            MenuScreen(
                                onStartGame = { config, players, difficulty ->
                                    val controller = GameViewModel(
                                        initialConfig = config,
                                        initialPlayers = players,
                                        aiDifficulty = difficulty,
                                        onStateChanged = { nextState ->
                                            if (nextState.isGameOver) {
                                                offlineStorage.clearSavedGame()
                                                savedGame = null
                                            } else {
                                                val toSave = SavedOfflineGame(gameState = nextState, aiDifficulty = difficulty)
                                                offlineStorage.saveGame(toSave)
                                                savedGame = toSave
                                            }
                                        }
                                    )
                                    val initialSave = SavedOfflineGame(gameState = controller.uiState.value.gameState, aiDifficulty = difficulty)
                                    offlineStorage.saveGame(initialSave)
                                    savedGame = initialSave
                                    activeController = controller
                                    currentScreen = AppScreen.GAME
                                },
                                onResumeGame = savedGame?.let { saved ->
                                    {
                                        val controller = GameViewModel(
                                            initialConfig = saved.gameState.config,
                                            initialPlayers = saved.gameState.players,
                                            aiDifficulty = saved.aiDifficulty,
                                            restoredState = saved.gameState,
                                            onStateChanged = { nextState ->
                                                if (nextState.isGameOver) {
                                                    offlineStorage.clearSavedGame()
                                                    savedGame = null
                                                } else {
                                                    val toSave = SavedOfflineGame(gameState = nextState, aiDifficulty = saved.aiDifficulty)
                                                    offlineStorage.saveGame(toSave)
                                                    savedGame = toSave
                                                }
                                            }
                                        )
                                        activeController = controller
                                        currentScreen = AppScreen.GAME
                                    }
                                },
                                onDiscardSavedGame = {
                                    offlineStorage.clearSavedGame()
                                    savedGame = null
                                },
                                savedGameSummary = savedSummary,
                                onStartOnline = { currentScreen = AppScreen.ONLINE_LOBBY },
                                settings = menuSettings,
                                onSettingsChange = { newSettings ->
                                    menuSettings = newSettings
                                    offlineStorage.savePreferences(
                                        offlineStorage.loadPreferences().copy(
                                            opponentType = newSettings.opponentType,
                                            aiDifficulty = newSettings.aiDifficulty,
                                            enableTactical = newSettings.enableTactical,
                                            sequenceToWin = newSettings.sequenceToWin,
                                        )
                                    )
                                },
                                isNightTheme = isNightTheme,
                                onToggleNightTheme = { isNightTheme = it },
                                soundEnabled = soundEnabled,
                                onToggleSound = { enabled ->
                                    soundEnabled = enabled
                                    offlineStorage.savePreferences(offlineStorage.loadPreferences().copy(soundEnabled = enabled))
                                },
                                vibrationEnabled = vibrationEnabled,
                                onToggleVibration = { enabled ->
                                    vibrationEnabled = enabled
                                    offlineStorage.savePreferences(offlineStorage.loadPreferences().copy(vibrationEnabled = enabled))
                                },
                                onStartTutorial = { currentScreen = AppScreen.TUTORIAL }
                            )
                        }

                        AppScreen.ONLINE_LOBBY -> OnlineLobbyScreen(
                            session = onlineSession,
                            initialPlayerName = onlineProfile.playerName,
                            serverConfigurationError = serverConfigurationError,
                            onConnect = { playerName ->
                                onlineSession?.close()
                                val resumeToken = onlineProfile.takeIf { it.playerName == playerName }?.sessionToken
                                val profile = OnlineProfile(playerName = playerName, sessionToken = resumeToken)
                                onlineProfile = profile
                                onlineProfileStore.save(profile)
                                onlineSession = OnlineSession(
                                    OnlineClient(
                                        playerName = playerName,
                                        scope = scope,
                                        url = onlineServerUrl.trim(),
                                        initialSessionToken = resumeToken,
                                        onSessionTokenChanged = { token ->
                                            onlineProfile = onlineProfile.copy(sessionToken = token)
                                            onlineProfileStore.save(onlineProfile)
                                        },
                                    ),
                                    scope,
                                ).also { it.start() }
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
                                        if (controller is OnlineGameController) {
                                            exitOnline()
                                        } else {
                                            controller.close()
                                            activeController = null
                                            currentScreen = AppScreen.MENU
                                            savedGame = offlineStorage.loadSavedGame()?.takeIf { !it.gameState.isGameOver }
                                        }
                                    }
                                )
                            }
                        }

                        AppScreen.TUTORIAL -> TutorialScreen(
                            onFinishTutorial = {
                                hasCompletedOnboarding = true
                                offlineStorage.savePreferences(offlineStorage.loadPreferences().copy(hasCompletedOnboarding = true))
                                currentScreen = AppScreen.MENU
                            }
                        )
                    }
                }
            }
        }
    }
}
