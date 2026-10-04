package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.runtime.Composable

// No vibration motor to drive on this platform.
@Composable
actual fun rememberImpactHaptics(): ImpactHaptics = NoImpactHaptics
