@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.androidpoet.halo.internal

import io.github.androidpoet.halo.LiveActivityBridge
import io.github.androidpoet.halo.internal.bridge.HaloBridgingProtocol
import io.github.androidpoet.halo.internal.bridge.halo_bridge_instance

/** [LiveActivityBridge] over the `HaloKMPBridge` class the HaloKMP Swift package provides. */
internal class ObjCLiveActivityBridge(
    private val target: HaloBridgingProtocol,
) : LiveActivityBridge {
    override fun areActivitiesEnabled(): Boolean = target.areActivitiesEnabled()

    override fun activeActivities(): List<String> = target.activeActivities().filterIsInstance<String>()

    override fun start(
        activityJson: String,
        staleAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    ) = target.startWithActivityJson(activityJson, staleAtEpochMillis, completion)

    override fun update(
        id: String,
        contentJson: String,
        staleAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    ) = target.updateWithId(id, contentJson, staleAtEpochMillis, completion)

    override fun end(
        id: String,
        finalContentJson: String?,
        dismissal: String,
        dismissAtEpochMillis: Long,
        completion: (String?, String?) -> Unit,
    ) = target.endWithId(id, finalContentJson, dismissal, dismissAtEpochMillis, completion)

    override fun setStateListener(listener: (String, String) -> Unit) =
        target.setStateListener { id, state -> if (id != null && state != null) listener(id, state) }
}

/** The bridge from the linked HaloKMP Swift package, or null when the app does not link it. */
internal fun discoverBridge(): LiveActivityBridge? = halo_bridge_instance()?.let(::ObjCLiveActivityBridge)
