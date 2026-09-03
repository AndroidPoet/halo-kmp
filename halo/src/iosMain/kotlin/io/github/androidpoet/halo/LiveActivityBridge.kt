package io.github.androidpoet.halo

/**
 * The Swift side of iOS live activities. ActivityKit is Swift-only, so the app
 * implements this interface (the shipped `HaloBridge.swift` forwards to
 * the `HaloKMP` Swift package) and registers it with
 * [Halo.register] at launch.
 *
 * Every JSON string uses the [LiveActivityCodec] wire format. Completion callbacks
 * receive `(null, null)` on success or `(code, message)` on failure, where `code`
 * is one of `unsupported`, `denied`, `too_many`, `too_large`, `not_found`, `platform`.
 */
public interface LiveActivityBridge {
    /** `ActivityAuthorizationInfo().areActivitiesEnabled`. */
    public fun areActivitiesEnabled(): Boolean

    /** Encoded activity records for every activity ActivityKit still tracks, content included. */
    public fun activeActivities(): List<String>

    /** Requests a new activity from the encoded record; `staleAtEpochMillis` of `0` means no stale date. */
    public fun start(
        activityJson: String,
        staleAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    )

    /** Pushes new content to a running activity. */
    public fun update(
        id: String,
        contentJson: String,
        staleAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    )

    /**
     * Ends an activity. `dismissal` is `default`, `immediate` or `after`; the
     * instant applies to `after` only.
     */
    public fun end(
        id: String,
        finalContentJson: String?,
        dismissal: String,
        dismissAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    )

    /** Installs the single listener that receives `(id, state)` with state `active`, `ended`, `dismissed` or `expired`. */
    public fun setStateListener(listener: (String, String) -> Unit)
}
