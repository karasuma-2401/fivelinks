package com.karasuma.fivelinks.fivelinks_cmp.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPlatformSoundPlayer(): PlatformSoundPlayer =
    remember { WasmJsSoundPlayer }

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private object WasmJsSoundPlayer : PlatformSoundPlayer {
    private val cache = HashMap<Int, String>()

    override fun playPcm16Mono(samples: ShortArray, sampleRate: Int) {
        if (samples.isEmpty()) return
        val key = samples.hashCode() * 31 + sampleRate
        val uri = cache.getOrPut(key) { SoundSynthesizer.toWavDataUri(samples, sampleRate) }
        playAudioUrl(uri)
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("(url) => { try { const a = new Audio(url); a.volume = 0.85; a.play().catch(() => {}); } catch (e) {} }")
private external fun playAudioUrl(url: String)

