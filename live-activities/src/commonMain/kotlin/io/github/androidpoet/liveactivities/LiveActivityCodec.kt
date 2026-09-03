package io.github.androidpoet.liveactivities

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * JSON wire format shared with the Swift package and with Android notification
 * extras. Flat keys, every field optional except `title`, unknown keys ignored,
 * so payloads written by one version decode on another.
 */
public object LiveActivityCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        }

    /** Encodes [content] to its JSON object. */
    public fun encodeContent(content: LiveActivityContent): String = json.encodeToString(ContentDto.serializer(), content.toDto())

    /** Decodes a JSON object produced by [encodeContent]; throws [LiveActivityException.InvalidContent] on malformed input. */
    public fun decodeContent(jsonString: String): LiveActivityContent = parse(ContentDto.serializer(), jsonString).toModel()

    /** Encodes [request] as an activity record with state `active`. */
    public fun encodeRequest(
        request: LiveActivityRequest,
        startedAtEpochMillis: Long,
    ): String = encodeActivity(request.toActivity(startedAtEpochMillis))

    /** Encodes a full [activity] record, including its state. */
    public fun encodeActivity(activity: LiveActivity): String = json.encodeToString(ActivityDto.serializer(), activity.toDto())

    /** Decodes a record produced by [encodeActivity] or by the Swift package. */
    public fun decodeActivity(jsonString: String): LiveActivity = parse(ActivityDto.serializer(), jsonString).toModel()

    private fun <T> parse(
        serializer: kotlinx.serialization.KSerializer<T>,
        jsonString: String,
    ): T =
        try {
            json.decodeFromString(serializer, jsonString)
        } catch (e: SerializationException) {
            throw LiveActivityException.InvalidContent("Malformed live activity JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw LiveActivityException.InvalidContent("Malformed live activity JSON: ${e.message}", e)
        }
}

internal fun LiveActivityRequest.toActivity(startedAtEpochMillis: Long): LiveActivity =
    LiveActivity(
        id = id,
        kind = kind,
        content = content,
        state = LiveActivityState.Active,
        startedAtEpochMillis = startedAtEpochMillis,
        attributes = attributes,
        deepLink = deepLink,
    )

@Serializable
internal data class ContentDto(
    val title: String,
    val subtitle: String? = null,
    val shortText: String? = null,
    val progressCurrent: Int? = null,
    val progressMax: Int? = null,
    val progressIndeterminate: Boolean? = null,
    val timerStartAt: Long? = null,
    val timerEndAt: Long? = null,
    val timerCountsDown: Boolean? = null,
    val timerPausedAt: Long? = null,
    val icon: String? = null,
    val accentColorArgb: Long? = null,
    val staleAt: Long? = null,
    val values: Map<String, String> = emptyMap(),
)

@Serializable
internal data class ActivityDto(
    val id: String,
    val kind: String = "default",
    val startedAt: Long = 0,
    val state: String = "active",
    val deepLink: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val content: ContentDto,
)

internal fun LiveActivityContent.toDto(): ContentDto =
    ContentDto(
        title = title,
        subtitle = subtitle,
        shortText = shortText,
        progressCurrent = (progress as? LiveActivityProgress.Determinate)?.current,
        progressMax = (progress as? LiveActivityProgress.Determinate)?.max,
        progressIndeterminate = if (progress is LiveActivityProgress.Indeterminate) true else null,
        timerStartAt = timer?.startAtEpochMillis,
        timerEndAt = timer?.endAtEpochMillis,
        timerCountsDown = timer?.countsDown,
        timerPausedAt = timer?.pausedAtEpochMillis,
        icon = icon,
        accentColorArgb = accentColorArgb,
        staleAt = staleAtEpochMillis,
        values = values,
    )

internal fun ContentDto.toModel(): LiveActivityContent =
    LiveActivityContent(
        title = title,
        subtitle = subtitle,
        shortText = shortText,
        progress =
            when {
                progressIndeterminate == true -> LiveActivityProgress.Indeterminate
                progressCurrent != null && progressMax != null -> LiveActivityProgress.Determinate(progressCurrent, progressMax)
                else -> null
            },
        timer =
            if (timerStartAt != null && timerEndAt != null) {
                LiveActivityTimer(
                    startAtEpochMillis = timerStartAt,
                    endAtEpochMillis = timerEndAt,
                    countsDown = timerCountsDown ?: true,
                    pausedAtEpochMillis = timerPausedAt,
                )
            } else {
                null
            },
        icon = icon,
        accentColorArgb = accentColorArgb,
        staleAtEpochMillis = staleAt,
        values = values,
    )

internal fun LiveActivity.toDto(): ActivityDto =
    ActivityDto(
        id = id,
        kind = kind,
        startedAt = startedAtEpochMillis,
        state = state.wireName,
        deepLink = deepLink,
        attributes = attributes,
        content = content.toDto(),
    )

internal fun ActivityDto.toModel(): LiveActivity =
    LiveActivity(
        id = id,
        kind = kind,
        content = content.toModel(),
        state = liveActivityStateFromWire(state),
        startedAtEpochMillis = startedAt,
        attributes = attributes,
        deepLink = deepLink,
    )

internal val LiveActivityState.wireName: String
    get() =
        when (this) {
            LiveActivityState.Active -> "active"
            LiveActivityState.Ended -> "ended"
            LiveActivityState.Dismissed -> "dismissed"
            LiveActivityState.Expired -> "expired"
        }

internal fun liveActivityStateFromWire(name: String): LiveActivityState =
    when (name) {
        "ended" -> LiveActivityState.Ended
        "dismissed" -> LiveActivityState.Dismissed
        "expired" -> LiveActivityState.Expired
        else -> LiveActivityState.Active
    }
