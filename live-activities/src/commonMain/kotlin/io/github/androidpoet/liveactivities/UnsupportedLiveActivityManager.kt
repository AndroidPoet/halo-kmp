package io.github.androidpoet.liveactivities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager for platforms without live activities (JVM desktop, macOS, browser).
 * Lets shared code compile and run everywhere; every call fails with
 * [LiveActivityException.Unsupported].
 */
public class UnsupportedLiveActivityManager : LiveActivityManager {
    override val isSupported: Boolean = false

    override val activities: StateFlow<List<LiveActivity>> = MutableStateFlow(emptyList())

    override suspend fun authorization(): LiveActivityAuthorization = LiveActivityAuthorization.Unsupported

    override suspend fun start(request: LiveActivityRequest): LiveActivityResult<LiveActivity> = unsupported()

    override suspend fun update(
        id: String,
        content: LiveActivityContent,
    ): LiveActivityResult<Unit> = unsupported()

    override suspend fun end(
        id: String,
        finalContent: LiveActivityContent?,
        dismissal: LiveActivityDismissal,
    ): LiveActivityResult<Unit> = unsupported()

    override suspend fun endAll(dismissal: LiveActivityDismissal): LiveActivityResult<Unit> = unsupported()

    override fun close(): Unit = Unit

    private fun unsupported(): LiveActivityResult.Failure = LiveActivityResult.Failure(LiveActivityException.Unsupported())
}
