package io.github.androidpoet.halo

import kotlin.random.Random

private const val ID_BYTES = 16

/** Returns a random id of 32 lowercase hex characters. */
public fun generateLiveActivityId(): String =
    Random.nextBytes(ID_BYTES).joinToString("") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
