package io.github.androidpoet.liveactivities.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.androidpoet.liveactivities.AndroidLiveActivityManager
import io.github.androidpoet.liveactivities.LiveActivityManager

@Composable
public actual fun rememberLiveActivityManager(): LiveActivityManager {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) { AndroidLiveActivityManager(context) }
    DisposableEffect(manager) { onDispose { manager.close() } }
    return manager
}
