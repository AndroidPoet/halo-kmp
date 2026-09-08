package io.github.androidpoet.halo

import io.github.androidpoet.halo.internal.discoverBridge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

private const val STATE_EVENT_BUFFER = 64

/**
 * Holds the [LiveActivityBridge] the managers talk to. Nothing to wire: the first
 * time a manager needs it, the bridge is discovered from the HaloKMP Swift package
 * the app links (its `HaloKMPBridge` class). [register] remains for apps that
 * bring their own ActivityKit code.
 */
public object Halo {
    private val bridgeState = MutableStateFlow<LiveActivityBridge?>(null)
    private val events = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = STATE_EVENT_BUFFER)
    private var discovered = false

    /** The bridge in use: the one passed to [register], else the HaloKMP package's, else null when neither is present. */
    public val bridge: LiveActivityBridge?
        get() {
            bridgeState.value?.let { return it }
            if (!discovered) {
                discovered = true
                discoverBridge()?.let(::register)
            }
            return bridgeState.value
        }

    /** `(id, state)` events fanned out from the bridge to every manager. */
    internal val stateEvents: SharedFlow<Pair<String, String>> get() = events

    /**
     * Replaces the bridge with your own implementation. Optional: without it the
     * HaloKMP Swift package's bridge is used. The library must be linked into exactly
     * one Kotlin framework; a second copy would hold its own, empty registry.
     */
    public fun register(bridge: LiveActivityBridge) {
        bridgeState.value = bridge
        bridge.setStateListener { id, state -> events.tryEmit(id to state) }
    }
}
