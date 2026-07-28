package com.karasuma.fivelinks.fivelinks_cmp.ai.export

import kotlinx.serialization.Serializable

@Serializable
data class TrainSample(
    val encoderVersion: Int,
    val state: FloatArray,
    val pi: FloatArray,
    var z: Float,
)
