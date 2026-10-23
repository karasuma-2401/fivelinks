package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.ui.window.ComposeUIViewController
import com.karasuma.fivelinks.fivelinks_cmp.online.IosOnlineProfileStore
import com.karasuma.fivelinks.fivelinks_cmp.online.ServerConfig

fun MainViewController(
    onlineServerUrl: String = ServerConfig.DEV_URL,
    requireSecureOnline: Boolean = false,
) = ComposeUIViewController {
    App(
        onlineServerUrl = onlineServerUrl,
        requireSecureOnline = requireSecureOnline,
        onlineProfileStore = IosOnlineProfileStore(),
    )
}
