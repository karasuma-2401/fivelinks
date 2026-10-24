package com.karasuma.fivelinks.fivelinks_cmp.audio

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Controller for game sound effects.
 * Hooked up for offline and online game events, ready to trigger real audio
 * assets once sound design is finalized.
 */
interface SoundController {
    fun playChipPlace()
    fun playChipSnipe()
    fun playDivineWipe()
    fun playSequenceComplete()
    fun playVictory()
    fun playDefeat()
}

object NoOpSoundController : SoundController {
    override fun playChipPlace() = Unit
    override fun playChipSnipe() = Unit
    override fun playDivineWipe() = Unit
    override fun playSequenceComplete() = Unit
    override fun playVictory() = Unit
    override fun playDefeat() = Unit
}

val LocalSoundController = staticCompositionLocalOf<SoundController> { NoOpSoundController }
val LocalSoundEnabled = staticCompositionLocalOf { true }
val LocalVibrationEnabled = staticCompositionLocalOf { true }

