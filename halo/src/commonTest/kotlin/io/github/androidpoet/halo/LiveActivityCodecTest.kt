package io.github.androidpoet.halo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LiveActivityCodecTest {
    private val full =
        LiveActivityContent(
            title = "Focus",
            subtitle = "Deep work",
            shortText = "25m",
            progress = LiveActivityProgress.Determinate(current = 5, max = 25),
            timer = LiveActivityTimer(startAtEpochMillis = 1_000, endAtEpochMillis = 2_000, countsDown = false, pausedAtEpochMillis = 1_500),
            icon = "timer",
            accentColorArgb = 0xFF4F46E5,
            staleAtEpochMillis = 3_000,
            values = mapOf("mode" to "pomodoro"),
        )

    @Test
    fun titleOnlyRoundTrips() {
        val content = LiveActivityContent(title = "Only title")
        assertEquals(content, LiveActivityCodec.decodeContent(LiveActivityCodec.encodeContent(content)))
    }

    @Test
    fun fullContentRoundTrips() {
        assertEquals(full, LiveActivityCodec.decodeContent(LiveActivityCodec.encodeContent(full)))
    }

    @Test
    fun indeterminateProgressRoundTrips() {
        val content = full.copy(progress = LiveActivityProgress.Indeterminate)
        assertEquals(content, LiveActivityCodec.decodeContent(LiveActivityCodec.encodeContent(content)))
    }

    @Test
    fun countdownTimerRoundTrips() {
        val content = full.copy(timer = LiveActivityTimer(startAtEpochMillis = 10, endAtEpochMillis = 20))
        assertEquals(content, LiveActivityCodec.decodeContent(LiveActivityCodec.encodeContent(content)))
    }

    @Test
    fun activityRecordRoundTripsWithState() {
        val activity =
            LiveActivity(
                id = "abc",
                kind = "focus",
                content = full,
                state = LiveActivityState.Expired,
                startedAtEpochMillis = 42,
                attributes = mapOf("session" to "1"),
                deepLink = "sample://focus",
            )
        assertEquals(activity, LiveActivityCodec.decodeActivity(LiveActivityCodec.encodeActivity(activity)))
    }

    @Test
    fun unknownKeysAndMissingOptionalsAreTolerated() {
        val decoded = LiveActivityCodec.decodeContent("""{"title":"T","futureField":1,"values":{}}""")
        assertEquals(LiveActivityContent(title = "T"), decoded)
        val record = LiveActivityCodec.decodeActivity("""{"id":"x","content":{"title":"T"}}""")
        assertEquals("default", record.kind)
        assertEquals(LiveActivityState.Active, record.state)
        assertNull(record.deepLink)
    }

    @Test
    fun malformedJsonFailsTyped() {
        assertFailsWith<LiveActivityException.InvalidContent> { LiveActivityCodec.decodeContent("{not json") }
        assertFailsWith<LiveActivityException.InvalidContent> { LiveActivityCodec.decodeContent("""{"subtitle":"no title"}""") }
    }

    @Test
    fun staleAtIsAFlatKey() {
        val encoded = LiveActivityCodec.encodeContent(full)
        check("\"staleAt\":3000" in encoded) { encoded }
    }
}
