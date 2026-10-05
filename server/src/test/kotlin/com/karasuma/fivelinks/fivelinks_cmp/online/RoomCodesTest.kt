package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomCodesTest {

    @Test
    fun codes_avoidLookAlikeCharacters() {
        val random = Random(1)
        repeat(1_000) {
            val code = RoomCodes.generate(random)
            assertEquals(RoomCodes.LENGTH, code.length)
            assertTrue(code.none { it in "O0I1" }, code)
            assertTrue(code.all { it in RoomCodes.ALPHABET }, code)
        }
    }

    @Test
    fun typedCodes_areTidiedUp() {
        assertEquals("K7QP2M", RoomCodes.normalize(" k7q-p2m "))
    }
}
