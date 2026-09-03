package io.github.androidpoet.liveactivities.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.androidpoet.liveactivities.LiveActivityManager
import io.github.androidpoet.liveactivities.UnsupportedLiveActivityManager

@Composable
public actual fun rememberLiveActivityManager(): LiveActivityManager = remember { UnsupportedLiveActivityManager() }
