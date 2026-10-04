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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.game.GameScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.menu.MenuSettings
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel

enum class AppScreen {
    MENU,
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
        var activeViewModel by remember { mutableStateOf<GameViewModel?>(null) }
        var menuSettings by remember { mutableStateOf(MenuSettings()) }

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
                                activeViewModel = GameViewModel(
                                    initialConfig = config,
                                    initialPlayers = players,
                                    aiDifficulty = difficulty
                                )
                                currentScreen = AppScreen.GAME
                            },
                            settings = menuSettings,
                            onSettingsChange = { menuSettings = it },
                            isNightTheme = isNightTheme,
                            onToggleNightTheme = { isNightTheme = it }
                        )
                    }

                    AppScreen.GAME -> {
                        val vm = activeViewModel ?: remember {
                            GameViewModel()
                        }
                        GameScreen(
                            viewModel = vm,
                            onExitToMenu = {
                                currentScreen = AppScreen.MENU
                            }
                        )
                    }
                }
            }
        }
    }
}
