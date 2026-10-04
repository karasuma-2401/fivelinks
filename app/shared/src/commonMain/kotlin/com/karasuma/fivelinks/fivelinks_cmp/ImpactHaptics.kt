package com.karasuma.fivelinks.fivelinks_cmp

import androidx.compose.runtime.Composable

/** Vibration cues for destructive moves; silent where the device cannot vibrate. */
interface ImpactHaptics {
    /** A light tick: the lightning bolt lands. */
    fun strike()

    /** A sharp double pulse: a sniped chip shatters. */
    fun shatter()

    /** A long, fading rumble: Thiên Phạt wipes the board. */
    fun quake()
}

object NoImpactHaptics : ImpactHaptics {
    override fun strike() = Unit
    override fun shatter() = Unit
    override fun quake() = Unit
}

@Composable
expect fun rememberImpactHaptics(): ImpactHaptics
