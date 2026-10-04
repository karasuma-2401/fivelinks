package com.karasuma.fivelinks.fivelinks_cmp.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.karasuma.fivelinks.fivelinks_cmp.App
import com.karasuma.fivelinks.fivelinks_cmp.ui.theme.fiveLinkAppIcon

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FiveLink - Bài Tây chiến thuật",
        icon = fiveLinkAppIcon(),
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}
