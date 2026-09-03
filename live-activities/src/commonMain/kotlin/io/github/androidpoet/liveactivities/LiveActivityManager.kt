package io.github.androidpoet.liveactivities

import kotlinx.coroutines.flow.StateFlow

/**
 * One common API to start, update and end a live activity: a system-surfaced,
 * glanceable status card backed by ActivityKit (Lock Screen + Dynamic Island) on
 * iOS and Android 16 Live Updates (promoted ongoing notification) on Android.
 *
 * Neither platform needs per-second updates: a [LiveActivityTimer] is rendered by
 * the system from absolute timestamps. Call [update] only when the content
 * itself changes.
 *
 * Obtain an instance with `rememberLiveActivityManager()` from the Compose
 * module, or construct the platform class (`AndroidLiveActivityManager`,
 * `IosLiveActivityManager`) directly. Call [close] when done observing.
 */
public interface LiveActivityManager {
    /**
     * Whether this platform can show live activities at all: OS floor met and, on
     * iOS, the Swift bridge registered. Evaluated on every read.
     */
    public val isSupported: Boolean

    /** Current authorization; may change while the app runs (user toggles settings). */
    public suspend fun authorization(): LiveActivityAuthorization

    /**
     * Activities this manager knows about: those started in this process plus
     * those restored from the platform after process death.
     */
    public val activities: StateFlow<List<LiveActivity>>

    /** Starts a new activity. Fails with [LiveActivityException.AlreadyActive] when the id is in use. */
    public suspend fun start(request: LiveActivityRequest): LiveActivityResult<LiveActivity>

    /** Replaces the content of a running activity. */
    public suspend fun update(
        id: String,
        content: LiveActivityContent,
    ): LiveActivityResult<Unit>

    /**
     * Ends a running activity. With [finalContent] the platform keeps showing the
     * final card until [dismissal] removes it; without it the card is removed now.
     */
    public suspend fun end(
        id: String,
        finalContent: LiveActivityContent? = null,
        dismissal: LiveActivityDismissal = LiveActivityDismissal.Default,
    ): LiveActivityResult<Unit>

    /** Ends every known activity. Returns the first failure, if any. */
    public suspend fun endAll(dismissal: LiveActivityDismissal = LiveActivityDismissal.Immediate): LiveActivityResult<Unit>

    /** Stops observing platform state. Running activities are left untouched. */
    public fun close()
}
