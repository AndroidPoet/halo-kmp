package io.github.androidpoet.halo.compose

import androidx.compose.runtime.Composable
import io.github.androidpoet.halo.LiveActivityManager

/**
 * Returns the [LiveActivityManager] for the current platform, remembered across
 * recompositions and closed when it leaves composition:
 *
 * - **Android** — `AndroidLiveActivityManager` on the application context with
 *   `Halo.androidConfig`.
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
