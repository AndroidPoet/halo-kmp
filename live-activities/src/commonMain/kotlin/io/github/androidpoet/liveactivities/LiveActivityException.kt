package io.github.androidpoet.liveactivities

/**
 * Typed failure carried by [LiveActivityResult.Failure]. Every subtype has a
 * stable numeric [code] for logging and analytics.
 */
public sealed class LiveActivityException(
    public val code: Int,
    override val message: String,
    override val cause: Throwable?,
) : Exception(message, cause) {
    /** This platform or OS version cannot show live activities, or the iOS bridge is not registered. */
    public class Unsupported(
        message: String = "Live activities are not supported on this platform",
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_UNSUPPORTED, message, cause)

    /** Notifications or live activities are disabled for the app. */
    public class NotAuthorized(
        message: String = "Live activities are not authorized for this app",
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_NOT_AUTHORIZED, message, cause)

    /** No running activity has the given id. */
    public class NotFound(
        id: String,
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_NOT_FOUND, "No live activity with id $id", cause)

    /** The request or content failed validation. */
    public class InvalidContent(
        message: String,
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_INVALID_CONTENT, message, cause)

    /** The platform refused to start another activity. */
    public class TooManyActivities(
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_TOO_MANY, "The platform limit on concurrent live activities was reached", cause)

    /** Encoded request exceeds the platform payload limit. */
    public class PayloadTooLarge(
        message: String = "Live activity payload exceeds the platform limit",
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_PAYLOAD_TOO_LARGE, message, cause)

    /** An activity with this id is already running. */
    public class AlreadyActive(
        id: String,
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_ALREADY_ACTIVE, "Live activity $id is already active", cause)

    /** The platform reported an error not covered by another subtype. */
    public class PlatformError(
        message: String,
        cause: Throwable? = null,
    ) : LiveActivityException(CODE_PLATFORM, message, cause)

    private companion object {
        const val CODE_UNSUPPORTED = 2001
        const val CODE_NOT_AUTHORIZED = 2002
        const val CODE_NOT_FOUND = 2003
        const val CODE_INVALID_CONTENT = 2004
        const val CODE_TOO_MANY = 2005
        const val CODE_PAYLOAD_TOO_LARGE = 2006
        const val CODE_ALREADY_ACTIVE = 2007
        const val CODE_PLATFORM = 2008
    }
}
