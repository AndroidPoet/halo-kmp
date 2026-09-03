package io.github.androidpoet.halo

/** Outcome of a live-activity call. Exhaustively `when` over it; no call throws for control flow. */
public sealed class LiveActivityResult<out T> {
    /** The call succeeded with [value]. */
    public data class Success<T>(
        public val value: T,
    ) : LiveActivityResult<T>()

    /** The call failed with a typed [error]. */
    public data class Failure(
        public val error: LiveActivityException,
    ) : LiveActivityResult<Nothing>()
}

/** Returns the value, or null on failure. */
public fun <T> LiveActivityResult<T>.getOrNull(): T? = (this as? LiveActivityResult.Success)?.value

/** Returns the error, or null on success. */
public fun <T> LiveActivityResult<T>.errorOrNull(): LiveActivityException? = (this as? LiveActivityResult.Failure)?.error
