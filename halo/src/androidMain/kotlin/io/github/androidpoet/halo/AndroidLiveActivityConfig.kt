package io.github.androidpoet.halo

import android.app.PendingIntent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationManagerCompat

/**
 * Android presentation settings for [AndroidLiveActivityManager].
 *
 * @property channelId notification channel every activity is posted to.
 * @property channelName user-visible channel name, applied on first creation only.
 * @property channelImportance importance applied on first creation only; channels are
 *   immutable afterwards.
 * @property smallIconRes status-bar icon; `0` uses the application icon.
 * @property iconResolver maps [LiveActivityContent.icon] to a drawable resource at
 *   call time. Resource names are never looked up reflectively, so release
 *   builds with resource shrinking keep working. `null` or a `0` result falls back
 *   to [smallIconRes].
 * @property contentIntent builds the tap action; when `null` a request's
 *   `deepLink` is opened with `ACTION_VIEW` inside this app.
 */
public class AndroidLiveActivityConfig(
    public val channelId: String = "live_activities",
    public val channelName: String = "Live activities",
    public val channelImportance: Int = NotificationManagerCompat.IMPORTANCE_DEFAULT,
    @param:DrawableRes public val smallIconRes: Int = 0,
    public val iconResolver: ((String) -> Int)? = null,
    public val contentIntent: ((LiveActivity) -> PendingIntent)? = null,
)

/** Process-wide defaults consumed by `rememberLiveActivityManager()` on Android. */
public object Halo {
    /** Configuration used when no explicit one is passed. Set it once at app start. */
    public var androidConfig: AndroidLiveActivityConfig = AndroidLiveActivityConfig()
}
