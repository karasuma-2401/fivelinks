package com.karasuma.fivelinks.fivelinks_cmp.audio

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformSoundPlayer(): PlatformSoundPlayer = NoOpPlatformSoundPlayer

