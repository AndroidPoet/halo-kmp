package io.github.androidpoet.liveactivities.compose

import androidx.compose.runtime.Composable
import io.github.androidpoet.liveactivities.LiveActivityManager

/**
 * Returns the [LiveActivityManager] for the current platform, remembered across
 * recompositions and closed when it leaves composition:
 *
 * - **Android** — `AndroidLiveActivityManager` on the application context with
 *   `LiveActivities.androidConfig`.
 * - **iOS** — `IosLiveActivityManager` over the registered Swift bridge.
 * - **JVM desktop / browser** — `UnsupportedLiveActivityManager`.
 *
 * ```kotlin
 * val liveActivities = rememberLiveActivityManager()
 * val result = liveActivities.start(LiveActivityRequest(content))
 * ```
 */
@Composable
public expect fun rememberLiveActivityManager(): LiveActivityManager
