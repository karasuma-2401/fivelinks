package com.karasuma.fivelinks.fivelinks_cmp.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Controller for game sound effects across offline and online matches.
 */
interface SoundController {
    fun playCardSelect()
    fun playChipPlace()
    fun playChipSnipe()
    fun playDivineWipe()
    fun playSequenceComplete()
    fun playVictory()
    fun playDefeat()
}

object NoOpSoundController : SoundController {
    override fun playCardSelect() = Unit
    override fun playChipPlace() = Unit
    override fun playChipSnipe() = Unit
    override fun playDivineWipe() = Unit
    override fun playSequenceComplete() = Unit
    override fun playVictory() = Unit
    override fun playDefeat() = Unit
}

interface PlatformSoundPlayer {
    fun playPcm16Mono(samples: ShortArray, sampleRate: Int = SoundSynthesizer.SAMPLE_RATE)
}

object NoOpPlatformSoundPlayer : PlatformSoundPlayer {
    override fun playPcm16Mono(samples: ShortArray, sampleRate: Int) = Unit
}

@Composable
expect fun rememberPlatformSoundPlayer(): PlatformSoundPlayer

@Composable
fun rememberSoundController(enabled: Boolean): SoundController {
    val player = rememberPlatformSoundPlayer()
    val enabledState = rememberUpdatedState(enabled)
    return remember(player) {
        object : SoundController {
            override fun playCardSelect() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.cardSelectPcm)
            }

            override fun playChipPlace() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.chipPlacePcm)
            }

            override fun playChipSnipe() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.chipSnipePcm)
            }

            override fun playDivineWipe() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.divineWipePcm)
            }

            override fun playSequenceComplete() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.sequenceCompletePcm)
            }

            override fun playVictory() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.victoryPcm)
            }

            override fun playDefeat() {
                if (enabledState.value) player.playPcm16Mono(SoundSynthesizer.defeatPcm)
            }
        }
    }
}

/**
 * Pure-Kotlin procedural synthesizer for crisp, tactile board-game sound effects (24kHz 16-bit mono PCM).
 */
object SoundSynthesizer {
    const val SAMPLE_RATE = 24000

    /** Soft paper-card flick when selecting a card in hand. */
    val cardSelectPcm: ShortArray by lazy {
        val durationSec = 0.055
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        val rng = Random(42)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 68.0)
            val freq = 680.0 - 280.0 * (t / durationSec)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val tone = sin(phase) * 0.55
            val crisp = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 130.0) * 0.45
            out[i] = ((tone + crisp) * env * 0.42 * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        out
    }

    /** Tactile acrylic/wooden poker-chip clack when placing a chip on the board. */
    val chipPlacePcm: ShortArray by lazy {
        val durationSec = 0.115
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        val rng = Random(101)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            // Initial contact click + secondary bounce 16ms later
            val click1 = exp(-t * 95.0) * (sin(2.0 * PI * 540.0 * t) * 0.65 + sin(2.0 * PI * 1180.0 * t) * 0.35)
            val t2 = (t - 0.016).coerceAtLeast(0.0)
            val click2 = if (t >= 0.016) {
                exp(-t2 * 75.0) * (sin(2.0 * PI * 760.0 * t2) * 0.55 + sin(2.0 * PI * 1520.0 * t2) * 0.30)
            } else 0.0
            val transientNoise = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 180.0) * 0.25
            val sample = (click1 + click2 * 0.7 + transientNoise) * 0.62
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        out
    }

    /** Sharp lightning strike & crystalline chip shatter for Snipe moves. */
    val chipSnipePcm: ShortArray by lazy {
        val durationSec = 0.34
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        val rng = Random(777)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 1480.0 * exp(-t * 7.2) + 140.0
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val zap = sin(phase) * exp(-t * 10.5) * 0.6
            val shatterStart = 0.085
            val shatter = if (t >= shatterStart) {
                val ts = t - shatterStart
                val burstEnv = exp(-ts * 16.0)
                val chime = sin(2.0 * PI * 1960.0 * ts) * 0.35 + sin(2.0 * PI * 2640.0 * ts) * 0.25
                val crackle = (rng.nextDouble() * 2.0 - 1.0) * exp(-ts * 24.0) * 0.45
                (chime + crackle) * burstEnv
            } else 0.0
            val sample = (zap + shatter) * 0.68
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        out
    }

    /** Majestic golden chord & shockwave for Thiên Phạt Hoàng Kim (Divine Wipe). */
    val divineWipePcm: ShortArray by lazy {
        val durationSec = 0.78
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        val chordFreqs = doubleArrayOf(261.63, 523.25, 659.25, 783.99, 987.77, 1174.66)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = (t / 0.035).coerceAtMost(1.0)
            val decay = exp(-t * 3.4)
            var sum = 0.0
            for (idx in chordFreqs.indices) {
                val f = chordFreqs[idx]
                val detune = 1.0 + (idx - 2.5) * 0.0012
                sum += sin(2.0 * PI * f * detune * t) * (1.0 / chordFreqs.size)
            }
            val subImpact = sin(2.0 * PI * (110.0 * exp(-t * 4.0) + 55.0) * t) * exp(-t * 6.5) * 0.45
            val sample = (sum * attack * decay * 0.85 + subImpact) * 0.72
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        out
    }

    /** 5-note ascending bell chime when completing a 5-chip sequence (C5-E5-G5-B5-C6). */
    val sequenceCompletePcm: ShortArray by lazy {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 987.77, 1046.50)
        synthesizeArpeggio(notes, noteStepSec = 0.075, tailSec = 0.28, gain = 0.60)
    }

    /** Bright major fanfare on match victory (G4-C5-E5-G5-C6). */
    val victoryPcm: ShortArray by lazy {
        val notes = doubleArrayOf(392.00, 523.25, 659.25, 783.99, 1046.50)
        synthesizeArpeggio(notes, noteStepSec = 0.095, tailSec = 0.42, gain = 0.65)
    }

    /** Gentle descending cadence on match defeat (E4-C4-A3). */
    val defeatPcm: ShortArray by lazy {
        val notes = doubleArrayOf(329.63, 261.63, 220.00)
        synthesizeArpeggio(notes, noteStepSec = 0.13, tailSec = 0.32, gain = 0.50)
    }

    private fun synthesizeArpeggio(
        notes: DoubleArray,
        noteStepSec: Double,
        tailSec: Double,
        gain: Double
    ): ShortArray {
        val totalSec = notes.size * noteStepSec + tailSec
        val count = (SAMPLE_RATE * totalSec).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            var sum = 0.0
            for (n in notes.indices) {
                val start = n * noteStepSec
                if (t >= start) {
                    val tn = t - start
                    val env = (tn / 0.008).coerceAtMost(1.0) * exp(-tn * 5.8)
                    val f = notes[n]
                    val bell = sin(2.0 * PI * f * tn) * 0.72 +
                        sin(2.0 * PI * f * 2.0 * tn) * 0.20 +
                        sin(2.0 * PI * f * 3.0 * tn) * 0.08
                    sum += bell * env
                }
            }
            val sample = (sum * gain * 0.55).coerceIn(-1.0, 1.0)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun toWavDataUri(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): String {
        val dataSize = samples.size * 2
        val wav = ByteArray(44 + dataSize)
        fun writeAscii(offset: Int, s: String) {
            for (i in s.indices) wav[offset + i] = s[i].code.toByte()
        }
        fun writeIntLE(offset: Int, v: Int) {
            wav[offset] = (v and 0xFF).toByte()
            wav[offset + 1] = ((v shr 8) and 0xFF).toByte()
            wav[offset + 2] = ((v shr 16) and 0xFF).toByte()
            wav[offset + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun writeShortLE(offset: Int, v: Int) {
            wav[offset] = (v and 0xFF).toByte()
            wav[offset + 1] = ((v shr 8) and 0xFF).toByte()
        }
        writeAscii(0, "RIFF")
        writeIntLE(4, 36 + dataSize)
        writeAscii(8, "WAVE")
        writeAscii(12, "fmt ")
        writeIntLE(16, 16)
        writeShortLE(20, 1) // PCM
        writeShortLE(22, 1) // Mono
        writeIntLE(24, sampleRate)
        writeIntLE(28, sampleRate * 2)
        writeShortLE(32, 2)
        writeShortLE(34, 16)
        writeAscii(36, "data")
        writeIntLE(40, dataSize)
        for (i in samples.indices) {
            writeShortLE(44 + i * 2, samples[i].toInt())
        }
        return "data:audio/wav;base64," + Base64.encode(wav)
    }
}

val LocalSoundController = staticCompositionLocalOf<SoundController> { NoOpSoundController }
val LocalSoundEnabled = staticCompositionLocalOf { true }
val LocalVibrationEnabled = staticCompositionLocalOf { true }
