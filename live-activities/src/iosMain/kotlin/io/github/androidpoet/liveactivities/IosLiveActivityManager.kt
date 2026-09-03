package io.github.androidpoet.liveactivities

import io.github.androidpoet.liveactivities.internal.ActivityStore
import io.github.androidpoet.liveactivities.internal.nowEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.UIKit.UIDevice
import kotlin.coroutines.resume

private const val MIN_MAJOR = 16
private const val MIN_MINOR = 2

/**
 * Live activities over ActivityKit through the registered [LiveActivityBridge].
 * Requires iOS 16.2 and the bridge; [isSupported] is re-evaluated on every read.
 *
 * Activities ActivityKit still tracks are restored, content included, the first
 * time the bridge is available. System-ended activities (the eight-hour ceiling)
 * surface as [LiveActivityState.Expired].
 */
public class IosLiveActivityManager internal constructor(
    private val bridgeProvider: () -> LiveActivityBridge?,
    stateEvents: Flow<Pair<String, String>>,
    private val osSupported: Boolean,
) : LiveActivityManager {
    /** Uses the bridge registered through [LiveActivities.register]. */
    public constructor() : this({ LiveActivities.bridge }, LiveActivities.stateEvents, isOsSupported())

    private val store = ActivityStore()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var seeded = false

    init {
        scope.launch { stateEvents.collect { (id, state) -> onStateEvent(id, state) } }
        seedIfPossible()
    }

    override val isSupported: Boolean
        get() = osSupported && bridgeProvider() != null

    override val activities: StateFlow<List<LiveActivity>> get() = store.activities

    override suspend fun authorization(): LiveActivityAuthorization {
        val bridge = bridgeProvider()
        return when {
            !osSupported || bridge == null -> LiveActivityAuthorization.Unsupported
            !bridge.areActivitiesEnabled() -> LiveActivityAuthorization.Denied
            else -> LiveActivityAuthorization.Authorized
        }
    }

    override suspend fun start(request: LiveActivityRequest): LiveActivityResult<LiveActivity> {
        request.validate()?.let { return LiveActivityResult.Failure(it) }
        val bridge = readyBridge() ?: return LiveActivityResult.Failure(LiveActivityException.Unsupported())
        if (!bridge.areActivitiesEnabled()) return LiveActivityResult.Failure(LiveActivityException.NotAuthorized())
        return mutex.withLock {
            if (store.get(request.id)?.state == LiveActivityState.Active) {
                return LiveActivityResult.Failure(LiveActivityException.AlreadyActive(request.id))
            }
            val activity = request.toActivity(nowEpochMillis())
            val staleAt = request.content.staleAtEpochMillis ?: 0
            val failure =
                await { done -> bridge.start(LiveActivityCodec.encodeActivity(activity), staleAt, done) }
            if (failure != null) return LiveActivityResult.Failure(failure)
            store.put(activity)
            LiveActivityResult.Success(activity)
        }
    }

    override suspend fun update(
        id: String,
        content: LiveActivityContent,
    ): LiveActivityResult<Unit> {
        content.validate()?.let { return LiveActivityResult.Failure(it) }
        val bridge = readyBridge() ?: return LiveActivityResult.Failure(LiveActivityException.Unsupported())
        return mutex.withLock {
            if (store.get(id)?.state != LiveActivityState.Active) {
                return LiveActivityResult.Failure(LiveActivityException.NotFound(id))
            }
            val failure =
                await { done -> bridge.update(id, LiveActivityCodec.encodeContent(content), content.staleAtEpochMillis ?: 0, done) }
            if (failure != null) return LiveActivityResult.Failure(failure)
            store.updateContent(id, content)
            LiveActivityResult.Success(Unit)
        }
    }

    override suspend fun end(
        id: String,
        finalContent: LiveActivityContent?,
        dismissal: LiveActivityDismissal,
    ): LiveActivityResult<Unit> {
        finalContent?.validate()?.let { return LiveActivityResult.Failure(it) }
        dismissal.validate(nowEpochMillis())?.let { return LiveActivityResult.Failure(it) }
        val bridge = readyBridge() ?: return LiveActivityResult.Failure(LiveActivityException.Unsupported())
        return mutex.withLock {
            if (store.get(id) == null) return LiveActivityResult.Failure(LiveActivityException.NotFound(id))
            val failure =
                await { done ->
                    bridge.end(
                        id,
                        finalContent?.let(LiveActivityCodec::encodeContent),
                        dismissal.wireName,
                        (dismissal as? LiveActivityDismissal.After)?.epochMillis ?: 0,
                        done,
                    )
                }
            if (failure != null) return LiveActivityResult.Failure(failure)
            val terminal = if (dismissal == LiveActivityDismissal.Immediate) LiveActivityState.Dismissed else LiveActivityState.Ended
            store.setState(id, terminal, finalContent)
            LiveActivityResult.Success(Unit)
        }
    }

    override suspend fun endAll(dismissal: LiveActivityDismissal): LiveActivityResult<Unit> {
        var failure: LiveActivityResult<Unit>? = null
        store.active().forEach { activity ->
            val result = end(activity.id, finalContent = null, dismissal = dismissal)
            if (result is LiveActivityResult.Failure && failure == null) failure = result
        }
        return failure ?: LiveActivityResult.Success(Unit)
    }

    override fun close() {
        scope.cancel()
    }

    private fun readyBridge(): LiveActivityBridge? {
        if (!osSupported) return null
        val bridge = bridgeProvider() ?: return null
        seedIfPossible()
        return bridge
    }

    private fun seedIfPossible() {
        if (seeded) return
        val bridge = bridgeProvider() ?: return
        seeded = true
        val restored = bridge.activeActivities().mapNotNull { runCatching { LiveActivityCodec.decodeActivity(it) }.getOrNull() }
        if (restored.isNotEmpty()) store.putAll(restored)
    }

    private fun onStateEvent(
        id: String,
        state: String,
    ) {
        val current = store.get(id) ?: return
        val next =
            when (state) {
                "active" -> LiveActivityState.Active
                "expired" -> LiveActivityState.Expired
                "ended", "dismissed" ->
                    if (current.state == LiveActivityState.Active) {
                        LiveActivityState.Expired
                    } else if (state == "dismissed") {
                        LiveActivityState.Dismissed
                    } else {
                        current.state
                    }
                else -> return
            }
        if (next != current.state) store.setState(id, next)
    }

    private suspend fun await(block: ((String?, String?) -> Unit) -> Unit): LiveActivityException? =
        suspendCancellableCoroutine { continuation ->
            block { code, message ->
                if (continuation.isActive) continuation.resume(code?.let { toException(it, message) })
            }
        }

    private fun toException(
        code: String,
        message: String?,
    ): LiveActivityException {
        val detail = message ?: "ActivityKit reported $code"
        return when (code) {
            "unsupported" -> LiveActivityException.Unsupported(detail)
            "denied" -> LiveActivityException.NotAuthorized(detail)
            "too_many" -> LiveActivityException.TooManyActivities()
            "too_large" -> LiveActivityException.PayloadTooLarge(detail)
            "not_found" -> LiveActivityException.PlatformError(detail)
            else -> LiveActivityException.PlatformError(detail)
        }
    }
}

private val LiveActivityDismissal.wireName: String
    get() =
        when (this) {
            LiveActivityDismissal.Default -> "default"
            LiveActivityDismissal.Immediate -> "immediate"
            is LiveActivityDismissal.After -> "after"
        }

internal fun isOsSupported(): Boolean {
    val parts =
        UIDevice.currentDevice.systemVersion
            .split('.')
            .mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    return major > MIN_MAJOR || (major == MIN_MAJOR && minor >= MIN_MINOR)
}
