package io.github.androidpoet.halo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeBridge(
    var enabled: Boolean = true,
    var failWith: Pair<String, String?>? = null,
    val restored: List<String> = emptyList(),
) : LiveActivityBridge {
    val started = mutableListOf<String>()
    val updated = mutableListOf<Pair<String, String>>()
    val ended = mutableListOf<Triple<String, String?, String>>()

    override fun areActivitiesEnabled(): Boolean = enabled

    override fun activeActivities(): List<String> = restored

    override fun start(
        activityJson: String,
        staleAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    ) {
        started += activityJson
        completion(failWith?.first, failWith?.second)
    }

    override fun update(
        id: String,
        contentJson: String,
        staleAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    ) {
        updated += id to contentJson
        completion(failWith?.first, failWith?.second)
    }

    override fun end(
        id: String,
        finalContentJson: String?,
        dismissal: String,
        dismissAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    ) {
        ended += Triple(id, finalContentJson, dismissal)
        completion(failWith?.first, failWith?.second)
    }

    override fun setStateListener(listener: (String, String) -> Unit) = Unit
}

private suspend fun awaitState(
    manager: LiveActivityManager,
    state: LiveActivityState,
) = withContext(Dispatchers.Default) {
    withTimeout(15_000) { manager.activities.first { list -> list.single().state == state } }
}

class IosLiveActivityManagerTest {
    private val events = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 8)
    private val content = LiveActivityContent("Focus", timer = LiveActivityTimer(1, 2))

    private fun manager(
        bridge: LiveActivityBridge?,
        os: Boolean = true,
    ) = IosLiveActivityManager({ bridge }, events, os)

    @Test
    fun unregisteredBridgeIsUnsupported() =
        runTest {
            val manager = manager(null)
            assertFalse(manager.isSupported)
            assertEquals(LiveActivityAuthorization.Unsupported, manager.authorization())
            assertIs<LiveActivityException.Unsupported>(manager.start(LiveActivityRequest(content)).errorOrNull())
        }

    @Test
    fun oldOsIsUnsupportedEvenWithBridge() =
        runTest {
            val manager = manager(FakeBridge(), os = false)
            assertFalse(manager.isSupported)
            assertIs<LiveActivityException.Unsupported>(manager.start(LiveActivityRequest(content)).errorOrNull())
        }

    @Test
    fun disabledActivitiesAreDenied() =
        runTest {
            val manager = manager(FakeBridge(enabled = false))
            assertEquals(LiveActivityAuthorization.Denied, manager.authorization())
            assertIs<LiveActivityException.NotAuthorized>(manager.start(LiveActivityRequest(content)).errorOrNull())
        }

    @Test
    fun startUpdateEndLifecycle() =
        runTest {
            val bridge = FakeBridge()
            val manager = manager(bridge)
            val started = manager.start(LiveActivityRequest(content, id = "a")).getOrNull()!!
            assertEquals(LiveActivityState.Active, started.state)
            assertEquals(1, manager.activities.value.size)
            assertEquals("a", LiveActivityCodec.decodeActivity(bridge.started.single()).id)

            val newContent = content.copy(title = "Focus 2")
            assertIs<LiveActivityResult.Success<Unit>>(manager.update("a", newContent))
            assertEquals(
                newContent,
                manager.activities.value
                    .single()
                    .content,
            )
            assertEquals(newContent, LiveActivityCodec.decodeContent(bridge.updated.single().second))

            assertIs<LiveActivityResult.Success<Unit>>(manager.end("a", finalContent = content.copy(title = "Done")))
            assertEquals(
                LiveActivityState.Ended,
                manager.activities.value
                    .single()
                    .state,
            )
            assertEquals(
                "Done",
                manager.activities.value
                    .single()
                    .content.title,
            )
            assertEquals("default", bridge.ended.single().third)
        }

    @Test
    fun duplicateStartIsAlreadyActive() =
        runTest {
            val manager = manager(FakeBridge())
            manager.start(LiveActivityRequest(content, id = "a"))
            assertIs<LiveActivityException.AlreadyActive>(manager.start(LiveActivityRequest(content, id = "a")).errorOrNull())
        }

    @Test
    fun unknownIdIsNotFound() =
        runTest {
            val manager = manager(FakeBridge())
            assertIs<LiveActivityException.NotFound>(manager.update("nope", content).errorOrNull())
            assertIs<LiveActivityException.NotFound>(manager.end("nope").errorOrNull())
        }

    @Test
    fun bridgeErrorCodesMapToExceptions() =
        runTest {
            val expected =
                mapOf(
                    "unsupported" to LiveActivityException.Unsupported::class,
                    "denied" to LiveActivityException.NotAuthorized::class,
                    "too_many" to LiveActivityException.TooManyActivities::class,
                    "too_large" to LiveActivityException.PayloadTooLarge::class,
                    "platform" to LiveActivityException.PlatformError::class,
                )
            expected.forEach { (code, type) ->
                val manager = manager(FakeBridge(failWith = code to "boom"))
                val error = manager.start(LiveActivityRequest(content)).errorOrNull()!!
                assertTrue(type.isInstance(error), "$code -> ${error::class}")
                assertTrue(manager.activities.value.isEmpty())
            }
        }

    @Test
    fun systemEndOfActiveActivityBecomesExpired() =
        runTest {
            val manager = manager(FakeBridge())
            manager.start(LiveActivityRequest(content, id = "a"))
            events.emit("a" to "ended")
            awaitState(manager, LiveActivityState.Expired)
            manager.close()
        }

    @Test
    fun libraryEndThenDismissEventBecomesDismissed() =
        runTest {
            val manager = manager(FakeBridge())
            manager.start(LiveActivityRequest(content, id = "a"))
            manager.end("a", finalContent = content)
            events.emit("a" to "dismissed")
            awaitState(manager, LiveActivityState.Dismissed)
            manager.close()
        }

    @Test
    fun restoreDecodesFullContent() =
        runTest {
            val record =
                LiveActivityCodec.encodeActivity(
                    LiveActivity("r", "focus", content.copy(title = "Restored"), LiveActivityState.Active, 5),
                )
            val manager = manager(FakeBridge(restored = listOf(record, "{broken")))
            assertEquals(
                "Restored",
                manager.activities.value
                    .single()
                    .content.title,
            )
            assertIs<LiveActivityResult.Success<Unit>>(manager.update("r", content))
        }
}
