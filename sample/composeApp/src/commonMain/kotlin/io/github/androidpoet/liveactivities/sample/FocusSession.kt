package io.github.androidpoet.liveactivities.sample

import io.github.androidpoet.liveactivities.LiveActivityContent
import io.github.androidpoet.liveactivities.LiveActivityManager
import io.github.androidpoet.liveactivities.LiveActivityProgress
import io.github.androidpoet.liveactivities.LiveActivityRequest
import io.github.androidpoet.liveactivities.LiveActivityResult
import io.github.androidpoet.liveactivities.LiveActivityState
import io.github.androidpoet.liveactivities.LiveActivityTimer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MINUTE_MILLIS = 60_000L
private const val SESSION_MINUTES = 25
private const val EXTENSION_MINUTES = 5
private const val ACCENT = 0xFF4F46E5

@OptIn(ExperimentalTime::class)
private fun now(): Long = Clock.System.now().toEpochMilliseconds()

/** The running focus session mirrored in the live activity. */
data class Session(
    val id: String,
    val startAt: Long,
    val endAt: Long,
    val pausedAt: Long?,
)

/** One action's outcome, as the sample logs it. */
data class LogLine(
    val label: String,
    val result: LiveActivityResult<*>,
) {
    override fun toString(): String =
        when (result) {
            is LiveActivityResult.Success -> "$label: ok"
            is LiveActivityResult.Failure -> "$label: ${result.error::class.simpleName} (${result.error.code}) ${result.error.message}"
        }
}

/** Drives the live activity for a focus session; every call returns the session to keep. */
class FocusSession(
    private val manager: LiveActivityManager,
) {
    suspend fun start(): Pair<Session?, LogLine> {
        val start = now()
        val end = start + SESSION_MINUTES * MINUTE_MILLIS
        val request =
            LiveActivityRequest(
                content = focusContent(start, end, paused = null),
                kind = "focus",
                attributes = mapOf("session" to "demo"),
                deepLink = "liveactivities://sample",
            )
        val result = manager.start(request)
        val session = (result as? LiveActivityResult.Success)?.let { Session(it.value.id, start, end, null) }
        return session to LogLine("start", result)
    }

    suspend fun extend(current: Session): Pair<Session, LogLine> {
        val extended = current.copy(endAt = current.endAt + EXTENSION_MINUTES * MINUTE_MILLIS)
        val result = manager.update(extended.id, extended.content())
        return (if (result is LiveActivityResult.Success) extended else current) to LogLine("+5 min", result)
    }

    suspend fun togglePause(current: Session): Pair<Session, LogLine> {
        val toggled = current.copy(pausedAt = if (current.pausedAt == null) now().coerceIn(current.startAt, current.endAt) else null)
        val result = manager.update(toggled.id, toggled.content())
        val label = if (toggled.pausedAt == null) "resume" else "pause"
        return (if (result is LiveActivityResult.Success) toggled else current) to LogLine(label, result)
    }

    suspend fun finish(current: Session): Pair<Session?, LogLine> {
        val result =
            manager.end(
                current.id,
                finalContent =
                    LiveActivityContent(
                        title = "Session complete",
                        subtitle = "Nice work",
                        progress = LiveActivityProgress.Determinate(SESSION_MINUTES, SESSION_MINUTES),
                        icon = "checkmark.circle.fill",
                        accentColorArgb = ACCENT,
                    ),
            )
        return (if (result is LiveActivityResult.Success) null else current) to LogLine("finish", result)
    }

    /** Adopts an activity restored after process death so Finish still works. */
    fun restored(): Session? {
        val active = manager.activities.value.firstOrNull { it.state == LiveActivityState.Active } ?: return null
        val timer = active.content.timer ?: return null
        return Session(active.id, timer.startAtEpochMillis, timer.endAtEpochMillis, timer.pausedAtEpochMillis)
    }

    private fun Session.content(): LiveActivityContent = focusContent(startAt, endAt, pausedAt)

    private fun focusContent(
        start: Long,
        end: Long,
        paused: Long?,
    ): LiveActivityContent {
        val minutes = ((end - start) / MINUTE_MILLIS).toInt()
        return LiveActivityContent(
            title = "Focus session",
            subtitle = if (paused != null) "Paused" else "Deep work · $minutes min",
            // shortText stays null so the status-bar chip / Dynamic Island shows the ticking timer.
            progress = LiveActivityProgress.Determinate(current = 0, max = minutes),
            timer = LiveActivityTimer(startAtEpochMillis = start, endAtEpochMillis = end, countsDown = true, pausedAtEpochMillis = paused),
            icon = "timer",
            accentColorArgb = ACCENT,
        )
    }
}
