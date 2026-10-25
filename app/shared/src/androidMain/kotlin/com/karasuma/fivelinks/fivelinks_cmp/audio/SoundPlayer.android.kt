package com.karasuma.fivelinks.fivelinks_cmp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.concurrent.Executors

@Composable
actual fun rememberPlatformSoundPlayer(): PlatformSoundPlayer =
    remember { AndroidSoundPlayer }

private object AndroidSoundPlayer : PlatformSoundPlayer {
    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "FiveLinks-Audio").apply { isDaemon = true }
    }

    override fun playPcm16Mono(samples: ShortArray, sampleRate: Int) {
        if (samples.isEmpty()) return
        executor.execute {
            runCatching {
                val byteSize = samples.size * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(byteSize)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                try {
                    track.write(samples, 0, samples.size)
                    track.play()
                    val durationMs = (samples.size * 1000L / sampleRate) + 60L
                    Thread.sleep(durationMs)
                } finally {
                    runCatching { track.stop() }
                    runCatching { track.release() }
                }
            }
        }
    }
}

