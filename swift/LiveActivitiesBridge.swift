// Copy this file into your iOS app target (not the widget extension) and
// replace `ComposeApp` with the name of the Kotlin framework that contains
// live-activities-kmp. It is the only source you copy: the attributes type
// lives in the LiveActivitiesKMP Swift package, linked by both targets.
//
// Then, at launch:
//     LiveActivities.shared.register(bridge: LiveActivitiesBridge())

import ComposeApp
import Foundation
import LiveActivitiesKMP

final class LiveActivitiesBridge: LiveActivityBridge {
    func areActivitiesEnabled() -> Bool {
        guard #available(iOS 16.2, *) else { return false }
        return LiveActivityKit.shared.areActivitiesEnabled
    }

    func activeActivities() -> [String] {
        guard #available(iOS 16.2, *) else { return [] }
        return LiveActivityKit.shared.activeActivities()
    }

    func start(activityJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        LiveActivityKit.shared.start(activityJson: activityJson, staleAtEpochMillis: staleAtEpochMillis, completion: completion)
    }

    func update(id: String, contentJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        LiveActivityKit.shared.update(id: id, contentJson: contentJson, staleAtEpochMillis: staleAtEpochMillis, completion: completion)
    }

    func end(
        id: String,
        finalContentJson: String?,
        dismissal: String,
        dismissAtEpochMillis: Int64,
        completion: @escaping (String?, String?) -> Void
    ) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        LiveActivityKit.shared.end(
            id: id,
            finalContentJson: finalContentJson,
            dismissal: dismissal,
            dismissAtEpochMillis: dismissAtEpochMillis,
            completion: completion
        )
    }

    func setStateListener(listener: @escaping (String, String) -> Void) {
        guard #available(iOS 16.2, *) else { return }
        LiveActivityKit.shared.setStateListener(listener)
    }
}
