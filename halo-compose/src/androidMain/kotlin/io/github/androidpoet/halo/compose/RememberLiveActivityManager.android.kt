package io.github.androidpoet.halo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.androidpoet.halo.AndroidLiveActivityManager
import io.github.androidpoet.halo.LiveActivityManager

@Composable
public actual fun rememberLiveActivityManager(): LiveActivityManager {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) { AndroidLiveActivityManager(context) }
    DisposableEffect(manager) { onDispose { manager.close() } }
    return manager
}
