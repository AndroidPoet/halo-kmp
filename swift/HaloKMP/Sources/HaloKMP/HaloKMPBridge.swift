import Foundation

/// Found by the Kotlin side at run time (`NSClassFromString("HaloKMPBridge")`), so an
/// app only has to link this package. Every method forwards to `HaloKit.shared`; the
/// selectors are the contract and must not change.
@objc(HaloKMPBridge)
public final class HaloKMPBridge: NSObject {
    @objc public override init() { super.init() }

    @objc public func areActivitiesEnabled() -> Bool {
        guard #available(iOS 16.2, *) else { return false }
        return HaloKit.shared.areActivitiesEnabled
    }

    @objc public func activeActivities() -> [String] {
        guard #available(iOS 16.2, *) else { return [] }
        return HaloKit.shared.activeActivities()
    }

    @objc public func start(activityJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        HaloKit.shared.start(activityJson: activityJson, staleAtEpochMillis: staleAtEpochMillis, completion: completion)
    }

    @objc public func update(id: String, contentJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard #available(iOS 16.2, *) else { return completion("unsupported", "iOS 16.2 or later is required") }
        HaloKit.shared.update(id: id, contentJson: contentJson, staleAtEpochMillis: staleAtEpochMillis, completion: completion)
    }

    @objc public func end(
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

    @objc public func setStateListener(_ listener: @escaping (String, String) -> Void) {
        guard #available(iOS 16.2, *) else { return }
        HaloKit.shared.setStateListener(listener)
    }
}
