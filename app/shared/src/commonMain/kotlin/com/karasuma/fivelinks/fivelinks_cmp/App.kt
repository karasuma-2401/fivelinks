package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.karasuma.fivelinks.fivelinks_cmp.ai.Difficulty
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameConfig
import com.karasuma.fivelinks.fivelinks_cmp.domain.Player
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.GameScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.screens.MenuScreen
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.BackgroundDark
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.FiveLinksTheme
import com.karasuma.fivelinks.fivelinks_cmp.ui.viewmodel.GameViewModel

enum class AppScreen {
    MENU,
    GAME
}

@Composable
fun App() {
    FiveLinksTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
        var activeViewModel by remember { mutableStateOf<GameViewModel?>(null) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            when (currentScreen) {
                AppScreen.MENU -> {
                    MenuScreen(
                        onStartGame = { config, players, difficulty ->
                            activeViewModel = GameViewModel(
                                initialConfig = config,
                                initialPlayers = players,
                                aiDifficulty = difficulty
                            )
                            currentScreen = AppScreen.GAME
                        }
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
