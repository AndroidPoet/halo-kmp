package io.github.androidpoet.halo

/** Largest encoded request accepted; keeps under ActivityKit's ~4 KB ceiling and Binder limits. */
public const val LIVE_ACTIVITY_MAX_PAYLOAD_BYTES: Int = 3072

/** Returns the first problem with this content, or null when it is valid. */
public fun LiveActivityContent.validate(): LiveActivityException? {
    if (title.isBlank()) return LiveActivityException.InvalidContent("title must not be blank")
    val progressProblem = progress?.validate()
    if (progressProblem != null) return progressProblem
    return timer?.validate()
}

/** Returns the first problem with this request, including payload size, or null when it is valid. */
public fun LiveActivityRequest.validate(): LiveActivityException? {
    if (id.isBlank()) return LiveActivityException.InvalidContent("id must not be blank")
    content.validate()?.let { return it }
    val bytes = LiveActivityCodec.encodeRequest(this, 0).encodeToByteArray().size
    if (bytes > LIVE_ACTIVITY_MAX_PAYLOAD_BYTES) {
        return LiveActivityException.PayloadTooLarge(
            "Encoded request is $bytes bytes; the limit is $LIVE_ACTIVITY_MAX_PAYLOAD_BYTES",
        )
    }
    return null
}

/** Returns a problem when the dismissal instant is already in the past. */
public fun LiveActivityDismissal.validate(nowEpochMillis: Long): LiveActivityException? =
    when (this) {
        is LiveActivityDismissal.After ->
            if (epochMillis < nowEpochMillis) LiveActivityException.InvalidContent("dismissal instant is in the past") else null
        LiveActivityDismissal.Default, LiveActivityDismissal.Immediate -> null
    }

private fun LiveActivityProgress.validate(): LiveActivityException? =
    when (this) {
        is LiveActivityProgress.Determinate ->
            when {
                max <= 0 -> LiveActivityException.InvalidContent("progress max must be > 0")
                current < 0 -> LiveActivityException.InvalidContent("progress current must be >= 0")
                current > max -> LiveActivityException.InvalidContent("progress current must be <= max")
                else -> null
            }
        LiveActivityProgress.Indeterminate -> null
    }

private fun LiveActivityTimer.validate(): LiveActivityException? {
    if (endAtEpochMillis <= startAtEpochMillis) return LiveActivityException.InvalidContent("timer end must be after start")
    val paused = pausedAtEpochMillis ?: return null
    if (paused < startAtEpochMillis || paused > endAtEpochMillis) {
        return LiveActivityException.InvalidContent("timer pausedAt must lie within [start, end]")
    }
    return null
}
