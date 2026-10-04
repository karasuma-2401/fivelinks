package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.runtime.Composable

// No system back button on this platform.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
