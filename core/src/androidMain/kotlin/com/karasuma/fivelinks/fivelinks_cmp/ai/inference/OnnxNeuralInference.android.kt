package com.karasuma.fivelinks.fivelinks_cmp.ai.inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * Android ONNX Runtime backend. Loads `models/model_v1.onnx` from the classpath / assets.
 */
class OnnxNeuralInference private constructor(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    override val encoderVersion: Int,
    private val channels: Int,
    private val height: Int,
    private val width: Int,
) : NeuralInference {

    override val isAvailable: Boolean = true

    override suspend fun infer(stateNchw: FloatArray): NeuralOutput = withContext(Dispatchers.Default) {
        val expected = channels * height * width
        require(stateNchw.size == expected) {
            "Expected state tensor size $expected, got ${stateNchw.size}"
        }
        val shape = longArrayOf(1, channels.toLong(), height.toLong(), width.toLong())
        OnnxTensor.createTensor(env, FloatBuffer.wrap(stateNchw), shape).use { input ->
            session.run(mapOf(INPUT_NAME to input)).use { result ->
                val logitsTensor = result.get(OUTPUT_POLICY).get() as OnnxTensor
                val valueTensor = result.get(OUTPUT_VALUE).get() as OnnxTensor
                val logits = logitsTensor.floatBuffer.let { buf ->
                    FloatArray(buf.remaining()).also { buf.get(it) }
                }
                val valueBuf = valueTensor.floatBuffer
                val value = valueBuf.get()
                NeuralOutput(policyLogits = logits, value = value)
            }
        }
    }

    fun close() {
        session.close()
    }

    companion object {
        const val MODEL_RESOURCE = "models/model_v1.onnx"
        const val INPUT_NAME = "state"
        const val OUTPUT_POLICY = "policy_logits"
        const val OUTPUT_VALUE = "value"

        fun createOrNull(
            encoderVersion: Int = 1,
            channels: Int = 12,
            height: Int = 10,
            width: Int = 10,
            resourcePath: String = MODEL_RESOURCE,
            modelBytes: ByteArray? = null,
        ): OnnxNeuralInference? {
            return try {
                val bytes = modelBytes ?: loadResource(resourcePath) ?: return null
                val env = OrtEnvironment.getEnvironment()
                val session = env.createSession(bytes, OrtSession.SessionOptions())
                OnnxNeuralInference(env, session, encoderVersion, channels, height, width)
            } catch (_: Throwable) {
                null
            }
        }

        private fun loadResource(path: String): ByteArray? {
            val stream = OnnxNeuralInference::class.java.classLoader?.getResourceAsStream(path)
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(path)
                ?: return null
            return stream.use { it.readBytes() }
        }
    }
}

actual fun createNeuralInferenceOrNull(): NeuralInference? =
    OnnxNeuralInference.createOrNull()
