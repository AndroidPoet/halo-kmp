package io.github.androidpoet.halo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveActivityIdTest {
    @Test
    fun idsAre32LowercaseHex() {
        repeat(100) {
            val id = generateLiveActivityId()
            assertEquals(32, id.length)
            assertTrue(id.all { it in '0'..'9' || it in 'a'..'f' }, id)
        }
    }

    @Test
    fun idsAreUnique() {
        val ids = List(1000) { generateLiveActivityId() }
        assertEquals(1000, ids.toSet().size)
    }
}
