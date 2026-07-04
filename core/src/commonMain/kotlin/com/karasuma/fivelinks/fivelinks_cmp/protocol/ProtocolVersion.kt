package com.karasuma.fivelinks.fivelinks_cmp.protocol

object ProtocolVersion {
    const val MAJOR = 1
    const val MINOR = 0
    const val STRING = "$MAJOR.$MINOR"
    override fun toString(): String {
        return STRING
    }
    fun majorCompatible(remote: String): Boolean {
        val major = remote.substringBefore('.').toIntOrNull() ?: return  false
        return major == MAJOR
    }
}