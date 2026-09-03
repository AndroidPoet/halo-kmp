package io.github.androidpoet.halo.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.androidpoet.halo.LiveActivity
import io.github.androidpoet.halo.LiveActivityAuthorization
import io.github.androidpoet.halo.compose.rememberLiveActivityManager
import kotlinx.coroutines.launch

private const val LOG_LINES = 8

/** Focus-session demo: one shared screen driving the platform live activity. */
@Composable
fun App() {
    MaterialTheme {
        val manager = rememberLiveActivityManager()
        val focus = remember(manager) { FocusSession(manager) }
        val scope = rememberCoroutineScope()
        val activities by manager.activities.collectAsState()
        var authorization by remember { mutableStateOf<LiveActivityAuthorization?>(null) }
        var log by remember { mutableStateOf(listOf<LogLine>()) }
        var session by remember { mutableStateOf<Session?>(null) }

        fun apply(outcome: Pair<Session?, LogLine>) {
            session = outcome.first
            log = (listOf(outcome.second) + log).take(LOG_LINES)
        }

        LaunchedEffect(manager) { authorization = manager.authorization() }
        LaunchedEffect(activities) { if (session == null) session = focus.restored() }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Halo", style = MaterialTheme.typography.headlineMedium)
            Text("Platform: ${platformName()}", style = MaterialTheme.typography.bodyMedium)
            Text("Supported: ${manager.isSupported} · Authorization: ${authorization ?: "…"}", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { scope.launch { authorization = manager.authorization() } }) { Text("Refresh authorization") }

            Spacer(Modifier.height(8.dp))
            Controls(
                session = session,
                onStart = { scope.launch { apply(focus.start()) } },
                onExtend = { current -> scope.launch { apply(focus.extend(current)) } },
                onTogglePause = { current -> scope.launch { apply(focus.togglePause(current)) } },
                onFinish = { current -> scope.launch { apply(focus.finish(current)) } },
            )

            Spacer(Modifier.height(8.dp))
            ActivityList(activities)

            Spacer(Modifier.height(8.dp))
            Text("Log", style = MaterialTheme.typography.titleMedium)
            log.forEach { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun Controls(
    session: Session?,
    onStart: () -> Unit,
    onExtend: (Session) -> Unit,
    onTogglePause: (Session) -> Unit,
    onFinish: (Session) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = session == null, onClick = onStart) { Text("Start 25 min") }
        Button(enabled = session != null, onClick = { session?.let(onExtend) }) { Text("+5 min") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = session != null, onClick = { session?.let(onTogglePause) }) {
            Text(if (session?.pausedAt == null) "Pause" else "Resume")
        }
        OutlinedButton(enabled = session != null, onClick = { session?.let(onFinish) }) { Text("Finish") }
    }
}

@Composable
private fun ActivityList(activities: List<LiveActivity>) {
    Text("Known activities", style = MaterialTheme.typography.titleMedium)
    if (activities.isEmpty()) Text("none", style = MaterialTheme.typography.bodySmall)
    activities.forEach { activity ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${activity.content.title} · ${activity.state}", style = MaterialTheme.typography.bodyMedium)
                Text(activity.id, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

expect fun platformName(): String
