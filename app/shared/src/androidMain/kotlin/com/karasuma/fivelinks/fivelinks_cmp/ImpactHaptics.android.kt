package com.karasuma.fivelinks.fivelinks_cmp

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImpactHaptics(): ImpactHaptics {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidImpactHaptics(context) }
}

/**
 * Drives the vibration motor directly (VIBRATE permission). The cues are tagged as
 * media/game vibrations: untagged short pulses count as touch feedback, which many
 * players switch off in system settings.
 */
private class AndroidImpactHaptics(context: Context) : ImpactHaptics {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun strike() = play(longArrayOf(0, 25), intArrayOf(0, 150))

    override fun shatter() = play(longArrayOf(0, 40, 45, 70), intArrayOf(0, 255, 0, 160))

    override fun quake() = play(
        longArrayOf(0, 150, 40, 120, 40, 90, 40, 60),
        intArrayOf(0, 255, 0, 210, 0, 150, 0, 90)
    )

    /** [timings] alternate off/on in ms; [amplitudes] (0-255) pair with them. */
    private fun play(timings: LongArray, amplitudes: IntArray) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> vibrator.vibrate(
                    VibrationEffect.createWaveform(timings, amplitudes, -1),
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA)
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> @Suppress("DEPRECATION") vibrator.vibrate(
                    VibrationEffect.createWaveform(timings, amplitudes, -1),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()
                )
                else -> @Suppress("DEPRECATION") vibrator.vibrate(timings, -1)
            }
        }
    }
}
