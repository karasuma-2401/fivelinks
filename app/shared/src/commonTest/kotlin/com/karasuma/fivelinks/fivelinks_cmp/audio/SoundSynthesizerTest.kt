package com.karasuma.fivelinks.fivelinks_cmp.audio

import kotlin.test.Test
import kotlin.test.assertTrue

class SoundSynthesizerTest {
    @Test
    fun allSoundEffectsGenerateValidPcmAndWavDataUri() {
        val effects = listOf(
            SoundSynthesizer.cardSelectPcm,
            SoundSynthesizer.chipPlacePcm,
            SoundSynthesizer.chipSnipePcm,
            SoundSynthesizer.divineWipePcm,
            SoundSynthesizer.sequenceCompletePcm,
            SoundSynthesizer.victoryPcm,
            SoundSynthesizer.defeatPcm,
        )
        for (samples in effects) {
            assertTrue(samples.isNotEmpty(), "PCM samples must not be empty")
            assertTrue(samples.any { it != 0.toShort() }, "PCM samples must contain non-zero waveform data")
            val wavUri = SoundSynthesizer.toWavDataUri(samples)
            assertTrue(wavUri.startsWith("data:audio/wav;base64,UklGR"), "WAV Data URI must start with RIFF header")
        }
    }
}

