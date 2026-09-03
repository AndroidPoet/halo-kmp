package io.github.androidpoet.halo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.androidpoet.halo.LiveActivityManager
import io.github.androidpoet.halo.UnsupportedLiveActivityManager

@Composable
public actual fun rememberLiveActivityManager(): LiveActivityManager = remember { UnsupportedLiveActivityManager() }
