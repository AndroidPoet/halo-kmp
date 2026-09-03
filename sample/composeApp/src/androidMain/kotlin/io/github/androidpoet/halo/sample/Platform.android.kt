package io.github.androidpoet.halo.sample

import android.os.Build

actual fun platformName(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
