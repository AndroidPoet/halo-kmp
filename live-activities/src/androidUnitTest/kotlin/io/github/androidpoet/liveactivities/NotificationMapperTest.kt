package io.github.androidpoet.liveactivities

import io.github.androidpoet.liveactivities.internal.NotificationMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationMapperTest {
    @Test
    fun defaultDismissalKeepsFinalCardFourHours() {
        assertEquals(4L * 60 * 60 * 1000, NotificationMapper.timeoutMillisFor(LiveActivityDismissal.Default, nowEpochMillis = 0))
    }

    @Test
    fun afterDismissalIsClampedDelta() {
        assertEquals(500, NotificationMapper.timeoutMillisFor(LiveActivityDismissal.After(1_500), nowEpochMillis = 1_000))
        assertEquals(0, NotificationMapper.timeoutMillisFor(LiveActivityDismissal.After(900), nowEpochMillis = 1_000))
        assertEquals(0, NotificationMapper.timeoutMillisFor(LiveActivityDismissal.Immediate, nowEpochMillis = 1_000))
    }

    @Test
    fun iconFallsBackWithoutResolver() {
        val config = AndroidLiveActivityConfig(smallIconRes = 7)
        assertEquals(7, NotificationMapper.resolveIconRes(config, iconKey = "timer", appIconRes = 3))
        assertEquals(3, NotificationMapper.resolveIconRes(AndroidLiveActivityConfig(), iconKey = "timer", appIconRes = 3))
    }

    @Test
    fun iconResolverWinsWhenItReturnsAResource() {
        val config = AndroidLiveActivityConfig(smallIconRes = 7, iconResolver = { key -> if (key == "timer") 11 else 0 })
        assertEquals(11, NotificationMapper.resolveIconRes(config, iconKey = "timer", appIconRes = 3))
        assertEquals(7, NotificationMapper.resolveIconRes(config, iconKey = "unknown", appIconRes = 3))
    }

    @Test
    fun extrasRecordRoundTripsThroughCodec() {
        val activity =
            LiveActivity(
                id = "id",
                kind = "focus",
                content = LiveActivityContent("Focus", timer = LiveActivityTimer(1, 2)),
                state = LiveActivityState.Ended,
                startedAtEpochMillis = 9,
            )
        assertEquals(activity, LiveActivityCodec.decodeActivity(LiveActivityCodec.encodeActivity(activity)))
    }
}
