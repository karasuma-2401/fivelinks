package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.ui.window.ComposeUIViewController
import com.karasuma.fivelinks.fivelinks_cmp.online.IosOnlineProfileStore
import com.karasuma.fivelinks.fivelinks_cmp.online.ServerConfig
import com.karasuma.fivelinks.fivelinks_cmp.storage.IosOfflineStorage

fun MainViewController(
    onlineServerUrl: String = ServerConfig.DEV_URL,
    requireSecureOnline: Boolean = false,
) = ComposeUIViewController {
    App(
        onlineServerUrl = onlineServerUrl,
        requireSecureOnline = requireSecureOnline,
        onlineProfileStore = IosOnlineProfileStore(),
        offlineStorage = IosOfflineStorage(),
    )
}
