package com.karasuma.fivelinks.fivelinks_cmp.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.karasuma.fivelinks.fivelinks_cmp.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FiveLinks - Sequence Card Game",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}
