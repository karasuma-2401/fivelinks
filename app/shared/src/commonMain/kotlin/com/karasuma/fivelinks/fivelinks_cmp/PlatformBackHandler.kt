package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.runtime.Composable

/** Intercepts the system back button/gesture on platforms that have one. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
