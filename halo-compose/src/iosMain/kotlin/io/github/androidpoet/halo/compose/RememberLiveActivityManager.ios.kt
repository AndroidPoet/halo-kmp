package io.github.androidpoet.halo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.androidpoet.halo.IosLiveActivityManager
import io.github.androidpoet.halo.LiveActivityManager

@Composable
public actual fun rememberLiveActivityManager(): LiveActivityManager {
    val manager = remember { IosLiveActivityManager() }
    DisposableEffect(manager) { onDispose { manager.close() } }
    return manager
}
