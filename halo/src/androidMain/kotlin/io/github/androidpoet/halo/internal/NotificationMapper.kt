package io.github.androidpoet.halo.internal

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat
import io.github.androidpoet.halo.AndroidLiveActivityConfig
import io.github.androidpoet.halo.LiveActivity
import io.github.androidpoet.halo.LiveActivityCodec
import io.github.androidpoet.halo.LiveActivityDismissal
import io.github.androidpoet.halo.LiveActivityProgress
import io.github.androidpoet.halo.LiveActivityTimer

/** Pure mapping from the common model to a notification. */
internal object NotificationMapper {
    const val EXTRA_LIVE_ACTIVITY = "io.github.androidpoet.halo.ACTIVITY"
    const val DEFAULT_TIMEOUT_MILLIS = 4L * 60L * 60L * 1000L

    fun timeoutMillisFor(
        dismissal: LiveActivityDismissal,
        nowEpochMillis: Long,
    ): Long =
        when (dismissal) {
            LiveActivityDismissal.Default -> DEFAULT_TIMEOUT_MILLIS
            LiveActivityDismissal.Immediate -> 0
            is LiveActivityDismissal.After -> (dismissal.epochMillis - nowEpochMillis).coerceAtLeast(0)
        }

    fun resolveIconRes(
        config: AndroidLiveActivityConfig,
        iconKey: String?,
        appIconRes: Int,
    ): Int {
        val resolved = iconKey?.let { key -> config.iconResolver?.invoke(key) } ?: 0
        if (resolved != 0) return resolved
        return if (config.smallIconRes != 0) config.smallIconRes else appIconRes
    }

    fun build(
        context: Context,
        config: AndroidLiveActivityConfig,
        activity: LiveActivity,
        ongoing: Boolean,
        timeoutMillis: Long,
    ): Notification {
        val content = activity.content
        val builder =
            NotificationCompat
                .Builder(context, config.channelId)
                .setSmallIcon(resolveIconRes(config, content.icon, context.applicationInfo.icon))
                .setContentTitle(content.title)
                .setContentText(content.subtitle)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setRequestPromotedOngoing(ongoing)
                .setCategory(if (content.progress != null) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_STATUS)
                .addExtras(Bundle().apply { putString(EXTRA_LIVE_ACTIVITY, LiveActivityCodec.encodeActivity(activity)) })
        content.shortText?.let(builder::setShortCriticalText)
        content.accentColorArgb?.let { builder.setColor(it.toInt()) }
        content.progress?.let { builder.setStyle(it.toStyle()) }
        content.timer?.let { builder.applyTimer(it) }
        if (timeoutMillis > 0) builder.setTimeoutAfter(timeoutMillis)
        contentIntent(context, config, activity)?.let(builder::setContentIntent)
        return builder.build()
    }

    private fun LiveActivityProgress.toStyle(): NotificationCompat.ProgressStyle =
        when (this) {
            is LiveActivityProgress.Determinate ->
                NotificationCompat
                    .ProgressStyle()
                    .setProgress(current)
                    .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(max)))
            LiveActivityProgress.Indeterminate -> NotificationCompat.ProgressStyle().setProgressIndeterminate(true)
        }

    private fun NotificationCompat.Builder.applyTimer(timer: LiveActivityTimer) {
        if (timer.pausedAtEpochMillis != null) {
            setUsesChronometer(false)
            setSubText("Paused")
            return
        }
        setWhen(if (timer.countsDown) timer.endAtEpochMillis else timer.startAtEpochMillis)
        setShowWhen(true)
        setUsesChronometer(true)
        setChronometerCountDown(timer.countsDown)
    }

    private fun contentIntent(
        context: Context,
        config: AndroidLiveActivityConfig,
        activity: LiveActivity,
    ): PendingIntent? {
        config.contentIntent?.let { return it(activity) }
        val link = activity.deepLink ?: return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).setPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            activity.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
