package com.karasuma.fivelinks.fivelinks_cmp.ai.inference

data class NeuralOutput(
    val policyLogits: FloatArray,
    val value: Float,
)

interface NeuralInference {
    val encoderVersion: Int
    val isAvailable: Boolean
    suspend fun infer(stateNchw: FloatArray): NeuralOutput
}

/** Factory called from common; returns null when ORT/model is unavailable. */
expect fun createNeuralInferenceOrNull(): NeuralInference?
