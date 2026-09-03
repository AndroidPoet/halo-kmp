package io.github.androidpoet.liveactivities.sample

import platform.UIKit.UIDevice

actual fun platformName(): String = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
