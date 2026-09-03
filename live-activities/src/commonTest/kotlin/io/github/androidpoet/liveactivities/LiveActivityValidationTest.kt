package io.github.androidpoet.liveactivities

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class LiveActivityValidationTest {
    private val ok = LiveActivityContent(title = "ok")

    @Test
    fun validContentPasses() {
        assertNull(ok.validate())
        assertNull(LiveActivityRequest(ok).validate())
    }

    @Test
    fun blankTitleRejected() {
        assertIs<LiveActivityException.InvalidContent>(ok.copy(title = "  ").validate())
    }

    @Test
    fun progressBoundsRejected() {
        assertIs<LiveActivityException.InvalidContent>(ok.copy(progress = LiveActivityProgress.Determinate(0, 0)).validate())
        assertIs<LiveActivityException.InvalidContent>(ok.copy(progress = LiveActivityProgress.Determinate(-1, 5)).validate())
        assertIs<LiveActivityException.InvalidContent>(ok.copy(progress = LiveActivityProgress.Determinate(6, 5)).validate())
        assertNull(ok.copy(progress = LiveActivityProgress.Determinate(5, 5)).validate())
    }

    @Test
    fun timerBoundsRejected() {
        assertIs<LiveActivityException.InvalidContent>(ok.copy(timer = LiveActivityTimer(10, 10)).validate())
        assertIs<LiveActivityException.InvalidContent>(ok.copy(timer = LiveActivityTimer(10, 5)).validate())
        assertIs<LiveActivityException.InvalidContent>(ok.copy(timer = LiveActivityTimer(10, 20, pausedAtEpochMillis = 25)).validate())
        assertNull(ok.copy(timer = LiveActivityTimer(10, 20, pausedAtEpochMillis = 15)).validate())
    }

    @Test
    fun oversizedRequestRejected() {
        val big = LiveActivityRequest(ok.copy(values = mapOf("blob" to "x".repeat(LIVE_ACTIVITY_MAX_PAYLOAD_BYTES))))
        assertIs<LiveActivityException.PayloadTooLarge>(big.validate())
    }

    @Test
    fun dismissalInPastRejected() {
        assertIs<LiveActivityException.InvalidContent>(LiveActivityDismissal.After(5).validate(nowEpochMillis = 10))
        assertNull(LiveActivityDismissal.After(15).validate(nowEpochMillis = 10))
        assertNull(LiveActivityDismissal.Default.validate(nowEpochMillis = 10))
    }
}
