package com.karasuma.fivelinks.fivelinks_cmp.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

@Composable
actual fun rememberPlatformSoundPlayer(): PlatformSoundPlayer =
    remember { JvmSoundPlayer }

private object JvmSoundPlayer : PlatformSoundPlayer {
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "FiveLinks-JvmAudio").apply { isDaemon = true }
    }

    override fun playPcm16Mono(samples: ShortArray, sampleRate: Int) {
        if (samples.isEmpty()) return
        executor.execute {
            runCatching {
                val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
                val info = DataLine.Info(SourceDataLine::class.java, format)
                if (!AudioSystem.isLineSupported(info)) return@execute
                val bytes = ByteArray(samples.size * 2)
                for (i in samples.indices) {
                    val v = samples[i].toInt()
                    bytes[i * 2] = (v and 0xFF).toByte()
                    bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                }
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(format, bytes.size.coerceAtLeast(4096))
                try {
                    line.start()
                    line.write(bytes, 0, bytes.size)
                    line.drain()
                } finally {
                    runCatching { line.stop() }
                    runCatching { line.close() }
                }
            }
        }
    }
}

