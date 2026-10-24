package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.karasuma.fivelinks.fivelinks_cmp.storage.WebOfflineStorage
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(
            onlineServerUrl = configuredServerUrl(),
            requireSecureOnline = !isLocalDevelopmentHost(),
            onlineProfileStore = WebOnlineProfileStore(),
            offlineStorage = WebOfflineStorage(),
        )
    }
}

private fun configuredServerUrl(): String =
    document.querySelector("meta[name='fivelinks-server-url']")
        ?.getAttribute("content")
        ?.trim()
        .orEmpty()

/** Only localhost may use an unencrypted development WebSocket. */
private fun isLocalDevelopmentHost(): Boolean =
    window.location.hostname.lowercase() in setOf("localhost", "127.0.0.1", "::1")
