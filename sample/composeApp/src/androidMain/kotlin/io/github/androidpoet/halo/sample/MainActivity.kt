package io.github.androidpoet.halo.sample

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import io.github.androidpoet.halo.AndroidLiveActivityConfig
import io.github.androidpoet.halo.Halo

class MainActivity : ComponentActivity() {
    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Halo.androidConfig =
            AndroidLiveActivityConfig(
                smallIconRes = R.drawable.ic_timer,
                iconResolver = { key ->
                    when (key) {
                        "timer" -> R.drawable.ic_timer
                        "checkmark.circle.fill" -> R.drawable.ic_check
                        else -> 0
                    }
                },
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { App() }
    }
}
