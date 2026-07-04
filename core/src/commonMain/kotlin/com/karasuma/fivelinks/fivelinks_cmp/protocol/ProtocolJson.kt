package com.karasuma.fivelinks.fivelinks_cmp.protocol

import kotlinx.serialization.json.Json

val ProtocolJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    useArrayPolymorphism = false
    classDiscriminator = "type"
}