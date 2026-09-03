package io.github.androidpoet.halo

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class UnsupportedLiveActivityManagerTest {
    @Test
    fun everyCallFailsUnsupported() =
        runTest {
            val manager = UnsupportedLiveActivityManager()
            assertFalse(manager.isSupported)
            assertEquals(LiveActivityAuthorization.Unsupported, manager.authorization())
            assertIs<LiveActivityException.Unsupported>(manager.start(LiveActivityRequest(LiveActivityContent("t"))).errorOrNull())
            assertIs<LiveActivityException.Unsupported>(manager.update("x", LiveActivityContent("t")).errorOrNull())
            assertIs<LiveActivityException.Unsupported>(manager.end("x").errorOrNull())
            assertIs<LiveActivityException.Unsupported>(manager.endAll().errorOrNull())
            assertEquals(emptyList(), manager.activities.value)
        }
}
