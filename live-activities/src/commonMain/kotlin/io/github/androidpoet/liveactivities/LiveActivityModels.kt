package io.github.androidpoet.liveactivities

/** Whether the app may currently show live activities. */
public enum class LiveActivityAuthorization {
    /** The platform will show activities started now. */
    Authorized,

    /** The user or system disabled them; guide the user to settings. */
    Denied,

    /** This platform or OS version cannot show live activities. */
    Unsupported,
}

/**
 * Everything needed to start an activity.
 *
 * @property content the initial content.
 * @property id unique per manager; defaults to a random 32-hex id.
 * @property kind grouping metadata forwarded to both platforms. Selects no UI.
 * @property attributes static values set once for the lifetime of the activity
 *   (iOS `ActivityAttributes`); use [LiveActivityContent.values] for changing data.
 * @property deepLink URL opened when the card is tapped.
 */
public data class LiveActivityRequest(
    val content: LiveActivityContent,
    val id: String = generateLiveActivityId(),
    val kind: String = "default",
    val attributes: Map<String, String> = emptyMap(),
    val deepLink: String? = null,
)

/**
 * The visible state of an activity. Every field except [title] is optional.
 *
 * @property title primary line.
 * @property subtitle secondary line.
 * @property shortText text for the status-bar chip (Android) or the Dynamic
 *   Island compact slot (iOS). That slot shows one thing: a non-null value
 *   replaces the ticking [timer] there, so leave it null to let the timer show.
 * @property progress determinate or indeterminate progress bar.
 * @property timer natively ticking countdown or count-up.
 * @property icon iOS: an SF Symbol name. Android: the key handed to
 *   `AndroidLiveActivityConfig.iconResolver`; never looked up by resource name.
 * @property accentColorArgb tint as `0xFFRRGGBB`.
 * @property staleAtEpochMillis iOS marks the activity stale after this instant
 *   (`ActivityContent.staleDate`). Ignored on Android.
 * @property values free-form data forwarded to a custom widget UI.
 */
public data class LiveActivityContent(
    val title: String,
    val subtitle: String? = null,
    val shortText: String? = null,
    val progress: LiveActivityProgress? = null,
    val timer: LiveActivityTimer? = null,
    val icon: String? = null,
    val accentColorArgb: Long? = null,
    val staleAtEpochMillis: Long? = null,
    val values: Map<String, String> = emptyMap(),
)

/** Progress bar shape. */
public sealed interface LiveActivityProgress {
    /** `current` out of `max`, both in the same unit; `0 <= current <= max`, `max > 0`. */
    public data class Determinate(
        val current: Int,
        val max: Int,
    ) : LiveActivityProgress

    /** Activity bar with no known end. */
    public data object Indeterminate : LiveActivityProgress
}

/**
 * A timer the system renders and ticks itself between two absolute instants.
 *
 * Both bounds are required: iOS renders a `ClosedRange<Date>`, so a countdown that
 * has already elapsed shows `0:00` instead of crashing the widget. A count-up
 * stops advancing at [endAtEpochMillis].
 *
 * @property startAtEpochMillis interval start.
 * @property endAtEpochMillis interval end; must be after the start.
 * @property countsDown `true` shows remaining time, `false` shows elapsed time.
 * @property pausedAtEpochMillis when non-null the timer is frozen at this instant.
 */
public data class LiveActivityTimer(
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val countsDown: Boolean = true,
    val pausedAtEpochMillis: Long? = null,
)

/** How long the final card stays visible after [LiveActivityManager.end]. */
public sealed interface LiveActivityDismissal {
    /**
     * Platform default. iOS keeps the card on the Lock Screen for up to four hours;
     * Android keeps it as a normal, swipeable notification that times out after
     * four hours on a best-effort basis.
     */
    public data object Default : LiveActivityDismissal

    /** Remove the card now. */
    public data object Immediate : LiveActivityDismissal

    /** Remove the card at the given instant. */
    public data class After(
        val epochMillis: Long,
    ) : LiveActivityDismissal
}

/** Lifecycle state of an activity. */
public enum class LiveActivityState {
    /** Shown and updatable. */
    Active,

    /** Ended by this library; the final card may still be visible. */
    Ended,

    /** Removed from screen. */
    Dismissed,

    /** Ended by the system, not by this library; on iOS after the eight-hour ceiling. */
    Expired,
}

/**
 * A known activity and its last content.
 *
 * @property id the id from the originating [LiveActivityRequest].
 * @property kind grouping metadata from the request.
 * @property content last content this manager applied or restored.
 * @property state lifecycle state.
 * @property startedAtEpochMillis when the activity was started.
 * @property attributes static values from the request.
 * @property deepLink URL from the request.
 */
public data class LiveActivity(
    val id: String,
    val kind: String,
    val content: LiveActivityContent,
    val state: LiveActivityState,
    val startedAtEpochMillis: Long,
    val attributes: Map<String, String> = emptyMap(),
    val deepLink: String? = null,
)
