package io.github.androidpoet.halo

import android.Manifest
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.androidpoet.halo.internal.ActivityStore
import io.github.androidpoet.halo.internal.NotificationMapper
import io.github.androidpoet.halo.internal.nowEpochMillis
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Live activities as Android notifications: promoted ongoing notifications
 * (Android 16 Live Updates) where the system allows it, plain ongoing
 * notifications elsewhere. API 26 and up.
 *
 * Each activity is one notification keyed by `tag = id, id = 0`, posted to the
 * channel from [config]. The channel is created on construction. Activities
 * still on screen after process death are restored from the notification
 * extras; nothing survives a reboot.
 *
 * Requesting `POST_NOTIFICATIONS` (API 33+) is the app's responsibility;
 * [authorization] reports the outcome.
 */
public class AndroidLiveActivityManager(
    context: Context,
    private val config: AndroidLiveActivityConfig = Halo.androidConfig,
) : LiveActivityManager {
    private val appContext: Context = context.applicationContext
    private val notifications = NotificationManagerCompat.from(appContext)
    private val store = ActivityStore()
    private val mutex = Mutex()

    init {
        notifications.createNotificationChannel(
            NotificationChannel(config.channelId, config.channelName, config.channelImportance),
        )
        restore()
    }

    override val isSupported: Boolean = true

    /** Whether the system currently lets this app promote notifications to Live Updates (Android 16+). */
    public val canPromote: Boolean
        get() = Build.VERSION.SDK_INT >= PROMOTED_SDK && notifications.canPostPromotedNotifications()

    /**
     * Whether the posted notification for [id] meets the system's promotion
     * criteria. Always `false` before Android 16 or for unknown ids.
     */
    public fun isPromotable(id: String): Boolean {
        if (Build.VERSION.SDK_INT < PROMOTED_SDK) return false
        val posted = notifications.activeNotifications.firstOrNull { it.tag == id } ?: return false
        return runCatching { posted.notification.hasPromotableCharacteristics() }.getOrDefault(false)
    }

    override val activities: StateFlow<List<LiveActivity>> get() = store.activities

    override suspend fun authorization(): LiveActivityAuthorization {
        val permissionMissing =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val channelBlocked = notifications.getNotificationChannel(config.channelId)?.importance == NotificationManagerCompat.IMPORTANCE_NONE
        return if (permissionMissing || !notifications.areNotificationsEnabled() || channelBlocked) {
            LiveActivityAuthorization.Denied
        } else {
            LiveActivityAuthorization.Authorized
        }
    }

    override suspend fun start(request: LiveActivityRequest): LiveActivityResult<LiveActivity> {
        request.validate()?.let { return LiveActivityResult.Failure(it) }
        if (authorization() == LiveActivityAuthorization.Denied) {
            return LiveActivityResult.Failure(LiveActivityException.NotAuthorized())
        }
        return mutex.withLock {
            if (store.get(request.id)?.state == LiveActivityState.Active) {
                return LiveActivityResult.Failure(LiveActivityException.AlreadyActive(request.id))
            }
            val activity = request.toActivity(nowEpochMillis())
            post(activity, ongoing = true, timeoutMillis = 0)
            store.put(activity)
            LiveActivityResult.Success(activity)
        }
    }

    override suspend fun update(
        id: String,
        content: LiveActivityContent,
    ): LiveActivityResult<Unit> {
        content.validate()?.let { return LiveActivityResult.Failure(it) }
        return mutex.withLock {
            val existing =
                store.get(id)?.takeIf { it.state == LiveActivityState.Active }
                    ?: return LiveActivityResult.Failure(LiveActivityException.NotFound(id))
            val updated = existing.copy(content = content)
            post(updated, ongoing = true, timeoutMillis = 0)
            store.put(updated)
            LiveActivityResult.Success(Unit)
        }
    }

    override suspend fun end(
        id: String,
        finalContent: LiveActivityContent?,
        dismissal: LiveActivityDismissal,
    ): LiveActivityResult<Unit> {
        finalContent?.validate()?.let { return LiveActivityResult.Failure(it) }
        val now = nowEpochMillis()
        dismissal.validate(now)?.let { return LiveActivityResult.Failure(it) }
        return mutex.withLock {
            val existing = store.get(id) ?: return LiveActivityResult.Failure(LiveActivityException.NotFound(id))
            if (finalContent == null || dismissal == LiveActivityDismissal.Immediate) {
                notifications.cancel(id, NOTIFICATION_ID)
                store.setState(id, LiveActivityState.Dismissed)
            } else {
                val ended = existing.copy(content = finalContent, state = LiveActivityState.Ended)
                post(ended, ongoing = false, timeoutMillis = NotificationMapper.timeoutMillisFor(dismissal, now))
                store.put(ended)
            }
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

    override fun close(): Unit = Unit

    private fun post(
        activity: LiveActivity,
        ongoing: Boolean,
        timeoutMillis: Long,
    ) {
        val notification = NotificationMapper.build(appContext, config, activity, ongoing, timeoutMillis)
        notifications.notify(activity.id, NOTIFICATION_ID, notification)
    }

    private fun restore() {
        val restored =
            notifications.activeNotifications.mapNotNull { posted ->
                val record = posted.notification.extras?.getString(NotificationMapper.EXTRA_LIVE_ACTIVITY) ?: return@mapNotNull null
                runCatching { LiveActivityCodec.decodeActivity(record) }.getOrNull()
            }
        if (restored.isNotEmpty()) store.putAll(restored)
    }

    private companion object {
        const val NOTIFICATION_ID = 0
        const val PROMOTED_SDK = 36
    }
}
