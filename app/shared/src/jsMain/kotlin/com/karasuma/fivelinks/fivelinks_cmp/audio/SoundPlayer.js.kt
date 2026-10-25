package com.karasuma.fivelinks.fivelinks_cmp.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlatformSoundPlayer(): PlatformSoundPlayer =
    remember { JsSoundPlayer }

private object JsSoundPlayer : PlatformSoundPlayer {
    private val cache = HashMap<Int, String>()

    override fun playPcm16Mono(samples: ShortArray, sampleRate: Int) {
        if (samples.isEmpty()) return
        val key = samples.hashCode() * 31 + sampleRate
        val uri = cache.getOrPut(key) { SoundSynthesizer.toWavDataUri(samples, sampleRate) }
        playAudioUrl(uri)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun playAudioUrl(url: String) {
    js("try { var a = new Audio(url); a.volume = 0.85; a.play().catch(function(){}); } catch(e) {}")
}

