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
import com.karasuma.fivelinks.fivelinks_cmp.audio.LocalSoundController
import com.karasuma.fivelinks.fivelinks_cmp.audio.LocalSoundEnabled
import com.karasuma.fivelinks.fivelinks_cmp.audio.LocalVibrationEnabled
import com.karasuma.fivelinks.fivelinks_cmp.audio.rememberSoundController
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import com.karasuma.fivelinks.fivelinks_cmp.model.PlayerSyncRequest
import com.karasuma.fivelinks.fivelinks_cmp.online.InMemoryOnlineProfileStore
import com.karasuma.fivelinks.fivelinks_cmp.online.LeaderboardClient
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineClient
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineGameController
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineProfile
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineProfileStore
import com.karasuma.fivelinks.fivelinks_cmp.online.OnlineSession
import com.karasuma.fivelinks.fivelinks_cmp.online.ServerConfig
import com.karasuma.fivelinks.fivelinks_cmp.storage.InMemoryOfflineStorage
import com.karasuma.fivelinks.fivelinks_cmp.storage.OfflineStorage
import com.karasuma.fivelinks.fivelinks_cmp.storage.SavedOfflineGame
import com.karasuma.fivelinks.fivelinks_cmp.ui.components.MessageBox
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
    val soundController = rememberSoundController(enabled = soundEnabled)
    var hasCompletedOnboarding by remember { mutableStateOf(initialPrefs.hasCompletedOnboarding) }
    var savedGame by remember {
        mutableStateOf(offlineStorage.loadSavedGame()?.takeIf { !it.gameState.isGameOver })
    }

    var menuSettings by remember {
        mutableStateOf(initialPrefs.toMenuSettings())
    }

    FiveLinksTheme(darkTheme = isNightTheme) {
        CompositionLocalProvider(
            LocalSoundController provides soundController,
            LocalSoundEnabled provides soundEnabled,
            LocalVibrationEnabled provides vibrationEnabled
        ) {
            var currentScreen by remember {
                mutableStateOf(AppScreen.MENU)
            }
            var showWelcomePrompt by remember {
                mutableStateOf(!hasCompletedOnboarding)
            }
            var activeController by remember { mutableStateOf<GameController?>(null) }
            var onlineSession by remember { mutableStateOf<OnlineSession?>(null) }
            var onlineProfile by remember { mutableStateOf(onlineProfileStore.load()) }
            val scope = rememberCoroutineScope()
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val leaderboardClient = remember(onlineServerUrl) { LeaderboardClient(defaultHttpServerUrl = onlineServerUrl) }
            val currentPlayerId = onlineProfile.sessionToken
                ?: "guest_${onlineProfile.playerName.hashCode().toUInt().toString(16)}"
            val serverConfigurationError = remember(onlineServerUrl, requireSecureOnline) {
                ServerConfig.validationError(onlineServerUrl, requireSecureOnline)
            }

            // Pre-warm the server (especially Render free tier) in the background on app launch
            LaunchedEffect(onlineServerUrl) {
                leaderboardClient.warmup(onlineServerUrl)
            }

            fun syncOfflineMatchResult(finalState: GameState, difficulty: Difficulty) {
                if (finalState.players.none { it.isAi }) return
                val humanTeam = finalState.players.firstOrNull { !it.isAi }?.team ?: Team.BLUE
                val isWin = finalState.winner == humanTeam
                val delta = when {
                    finalState.winner == null -> 2
                    isWin -> when (difficulty) {
                        Difficulty.HARD -> 15
                        Difficulty.MEDIUM -> 10
                        Difficulty.EASY -> 5
                    }
                    else -> when (difficulty) {
                        Difficulty.HARD -> -8
                        Difficulty.MEDIUM -> -5
                        Difficulty.EASY -> 0
                    }
                }
                val seqCount = finalState.sequencesOf(humanTeam)
                scope.launch {
                    leaderboardClient.syncPlayerStats(
                        PlayerSyncRequest(
                            playerId = currentPlayerId,
                            playerName = onlineProfile.playerName,
                            ratingDelta = delta,
                            isWin = isWin,
                            sequencesCount = seqCount,
                            snipesCount = 0
                        )
                    )
                }
            }

            LaunchedEffect(onlineSession) {
                val session = onlineSession ?: return@LaunchedEffect
                var lastSyncedKey: String? = null
                session.state.collect { onlineState ->
                    val res = onlineState.result ?: return@collect
                    val game = onlineState.game ?: return@collect
                    val roomCode = onlineState.room?.code ?: ""
                    val syncKey = "$roomCode:${game.view.turnNumber}:${res.winner}:${res.reason}"
                    if (syncKey == lastSyncedKey) return@collect
                    lastSyncedKey = syncKey

                    val myTeam = game.view.players.firstOrNull { it.id == game.view.viewerId }?.team ?: return@collect
                    val isWin = res.winner != null && res.winner == myTeam
                    val delta = when {
                        res.winner == null -> 5
                        isWin -> 25
                        else -> -15
                    }
                    val seqCount = game.state.sequencesOf(myTeam)
                    leaderboardClient.syncPlayerStats(
                        PlayerSyncRequest(
                            playerId = currentPlayerId,
                            playerName = onlineProfile.playerName,
                            ratingDelta = delta,
                            isWin = isWin,
                            sequencesCount = seqCount,
                            snipesCount = 0
                        )
                    )
                }
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
                                    var syncedTurn = -1
                                    val controller = GameViewModel(
                                        initialConfig = config,
                                        initialPlayers = players,
                                        aiDifficulty = difficulty,
                                        onStateChanged = { nextState ->
                                            if (nextState.isGameOver) {
                                                offlineStorage.clearSavedGame()
                                                savedGame = null
                                                if (syncedTurn != nextState.turnNumber) {
                                                    syncedTurn = nextState.turnNumber
                                                    syncOfflineMatchResult(nextState, difficulty)
                                                }
                                            } else {
                                                val prevElapsed = savedGame?.elapsedSeconds ?: 0
                                                val toSave = SavedOfflineGame(
                                                    gameState = nextState,
                                                    aiDifficulty = difficulty,
                                                    elapsedSeconds = prevElapsed
                                                )
                                                offlineStorage.saveGame(toSave)
                                                savedGame = toSave
                                            }
                                        }
                                    )
                                    val initialSave = SavedOfflineGame(
                                        gameState = controller.uiState.value.gameState,
                                        aiDifficulty = difficulty,
                                        elapsedSeconds = 0
                                    )
                                    offlineStorage.saveGame(initialSave)
                                    savedGame = initialSave
                                    activeController = controller
                                    currentScreen = AppScreen.GAME
                                },
                                onResumeGame = savedGame?.let { saved ->
                                    {
                                        var syncedTurn = -1
                                        val controller = GameViewModel(
                                            initialConfig = saved.gameState.config,
                                            initialPlayers = saved.gameState.players,
                                            aiDifficulty = saved.aiDifficulty,
                                            restoredState = saved.gameState,
                                            onStateChanged = { nextState ->
                                                if (nextState.isGameOver) {
                                                    offlineStorage.clearSavedGame()
                                                    savedGame = null
                                                    if (syncedTurn != nextState.turnNumber) {
                                                        syncedTurn = nextState.turnNumber
                                                        syncOfflineMatchResult(nextState, saved.aiDifficulty)
                                                    }
                                                } else {
                                                    val prevElapsed = savedGame?.elapsedSeconds ?: saved.elapsedSeconds
                                                    val toSave = SavedOfflineGame(
                                                        gameState = nextState,
                                                        aiDifficulty = saved.aiDifficulty,
                                                        elapsedSeconds = prevElapsed
                                                    )
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
                                    if (enabled) soundController.playChipPlace()
                                },
                                vibrationEnabled = vibrationEnabled,
                                onToggleVibration = { enabled ->
                                    vibrationEnabled = enabled
                                    offlineStorage.savePreferences(offlineStorage.loadPreferences().copy(vibrationEnabled = enabled))
                                },
                                onStartTutorial = {
                                    showWelcomePrompt = false
                                    currentScreen = AppScreen.TUTORIAL
                                },
                                playerId = currentPlayerId,
                                playerName = onlineProfile.playerName,
                                serverUrl = onlineServerUrl,
                            )

                            if (showWelcomePrompt) {
                                MessageBox(
                                    title = "Chào mừng đến với FiveLink!",
                                    message = "Cờ bài chiến thuật kết hợp bài Tây & 5 quân liên tiếp. Bạn có muốn xem hướng dẫn nhanh (1 phút) để nắm cách đánh và ghép bài đặc biệt trước khi bắt đầu không?",
                                    confirmText = "Hướng dẫn",
                                    dismissText = "Bỏ qua",
                                    onConfirm = {
                                        showWelcomePrompt = false
                                        currentScreen = AppScreen.TUTORIAL
                                    },
                                    onDismiss = {
                                        showWelcomePrompt = false
                                        hasCompletedOnboarding = true
                                        offlineStorage.savePreferences(
                                            offlineStorage.loadPreferences().copy(hasCompletedOnboarding = true)
                                        )
                                    }
                                )
                            }
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
                                val initialElapsed = remember(controller) {
                                    if (controller is OnlineGameController) 0 else (savedGame?.elapsedSeconds ?: 0)
                                }
                                GameScreen(
                                    controller = controller,
                                    initialElapsedSeconds = initialElapsed,
                                    onElapsedSecondsChanged = { sec ->
                                        if (controller !is OnlineGameController) {
                                            val currentSaved = savedGame
                                            if (currentSaved != null && !currentSaved.gameState.isGameOver) {
                                                val updated = currentSaved.copy(elapsedSeconds = sec)
                                                offlineStorage.saveGame(updated)
                                                savedGame = updated
                                            }
                                        }
                                    },
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
