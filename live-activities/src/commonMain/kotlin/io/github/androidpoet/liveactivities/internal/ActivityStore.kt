package io.github.androidpoet.liveactivities.internal

import io.github.androidpoet.liveactivities.LiveActivity
import io.github.androidpoet.liveactivities.LiveActivityContent
import io.github.androidpoet.liveactivities.LiveActivityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-memory registry behind [io.github.androidpoet.liveactivities.LiveActivityManager.activities]. */
internal class ActivityStore {
    private val state = MutableStateFlow<List<LiveActivity>>(emptyList())

    val activities: StateFlow<List<LiveActivity>> get() = state

    fun get(id: String): LiveActivity? = state.value.firstOrNull { it.id == id }

    fun active(): List<LiveActivity> = state.value.filter { it.state == LiveActivityState.Active }

    fun put(activity: LiveActivity) {
        state.update { list -> list.filterNot { it.id == activity.id } + activity }
    }

    fun putAll(activities: List<LiveActivity>) {
        state.update { list ->
            val incoming = activities.map { it.id }.toSet()
            list.filterNot { it.id in incoming } + activities
        }
    }

    fun updateContent(
        id: String,
        content: LiveActivityContent,
    ): LiveActivity? {
        var updated: LiveActivity? = null
        state.update { list ->
            list.map { activity ->
                if (activity.id == id) activity.copy(content = content).also { updated = it } else activity
            }
        }
        return updated
    }

    fun setState(
        id: String,
        newState: LiveActivityState,
        content: LiveActivityContent? = null,
    ): LiveActivity? {
        var updated: LiveActivity? = null
        state.update { list ->
            list.map { activity ->
                if (activity.id == id) {
                    activity.copy(state = newState, content = content ?: activity.content).also { updated = it }
                } else {
                    activity
                }
            }
        }
        return updated
    }

    fun remove(id: String) {
        state.update { list -> list.filterNot { it.id == id } }
    }
}
