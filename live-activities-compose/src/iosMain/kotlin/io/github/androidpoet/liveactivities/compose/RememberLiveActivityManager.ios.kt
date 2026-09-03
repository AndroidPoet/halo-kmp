package io.github.androidpoet.liveactivities.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.androidpoet.liveactivities.IosLiveActivityManager
import io.github.androidpoet.liveactivities.LiveActivityManager

@Composable
public actual fun rememberLiveActivityManager(): LiveActivityManager {
    val manager = remember { IosLiveActivityManager() }
    DisposableEffect(manager) { onDispose { manager.close() } }
    return manager
}
