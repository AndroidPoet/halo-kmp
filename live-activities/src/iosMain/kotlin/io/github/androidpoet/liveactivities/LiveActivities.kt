package io.github.androidpoet.liveactivities

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

private const val STATE_EVENT_BUFFER = 64

/**
 * Holds the registered [LiveActivityBridge]. Managers read it on every call, so
 * registering after the first manager was created still works.
 */
public object LiveActivities {
    private val bridgeState = MutableStateFlow<LiveActivityBridge?>(null)
    private val events = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = STATE_EVENT_BUFFER)

    /** The registered bridge, or null until [register] runs. */
    public val bridge: LiveActivityBridge?
        get() = bridgeState.value

    /** `(id, state)` events fanned out from the bridge to every manager. */
    internal val stateEvents: SharedFlow<Pair<String, String>> get() = events

    /**
     * Registers the Swift bridge. Call once at app launch, from the glue file. The
     * library must be linked into exactly one Kotlin framework; a second copy
     * would hold its own, empty registry.
     */
    public fun register(bridge: LiveActivityBridge) {
        bridgeState.value = bridge
        bridge.setStateListener { id, state -> events.tryEmit(id to state) }
    }
}
