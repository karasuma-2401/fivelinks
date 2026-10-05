package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlin.random.Random

/** Six-character room codes without the look-alikes O/0 and I/1. */
object RoomCodes {
    const val LENGTH = 6
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(random: Random): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** What a player typed, tidied up: case, spaces and dashes do not matter. */
    fun normalize(input: String): String = input.uppercase().filterNot { it.isWhitespace() || it == '-' }
}
