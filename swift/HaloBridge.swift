// Copy this file into your iOS app target (not the widget extension) and
// replace `ComposeApp` with the name of the Kotlin framework that contains
// halo-kmp. It is the only source you copy: the attributes type
// lives in the HaloKMP Swift package, linked by both targets.
//
// Then, at launch:
//     Halo.shared.register(bridge: HaloBridge())

import ComposeApp
import Foundation
import HaloKMP

final class HaloBridge: LiveActivityBridge {
    func areActivitiesEnabled() -> Bool {
        guard #available(iOS 16.2, *) else { return false }
        return HaloKit.shared.areActivitiesEnabled
    }

    func activeActivities() -> [String] {
        guard #available(iOS 16.2, *) else { return [] }
        return HaloKit.shared.activeActivities()
    }

    func start(activityJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        HaloKit.shared.start(activityJson: activityJson, staleAtEpochMillis: staleAtEpochMillis, completion: completion)
    }

    func update(id: String, contentJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        HaloKit.shared.update(id: id, contentJson: contentJson, staleAtEpochMillis: staleAtEpochMillis, completion: completion)
    }

    func end(
        id: String,
        finalContentJson: String?,
        dismissal: String,
        dismissAtEpochMillis: Int64,
        completion: @escaping (String?, String?) -> Void
    ) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        HaloKit.shared.end(
            id: id,
            finalContentJson: finalContentJson,
            dismissal: dismissal,
            dismissAtEpochMillis: dismissAtEpochMillis,
            completion: completion
        )
    }

    func setStateListener(listener: @escaping (String, String) -> Void) {
        guard #available(iOS 16.2, *) else { return }
        HaloKit.shared.setStateListener(listener)
    }
}
