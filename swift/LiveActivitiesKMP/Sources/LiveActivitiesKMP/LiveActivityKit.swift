import ActivityKit
import Foundation

/// ActivityKit calls behind the Kotlin `LiveActivityBridge`. Thread-safe; the
/// app's bridge glue forwards to `LiveActivityKit.shared` from any thread.
///
/// Error codes handed to completions: `unsupported`, `denied`, `too_many`,
/// `too_large`, `not_found`, `platform`. States handed to the listener:
/// `active`, `ended`, `dismissed`, `expired` (ended by the system, not by
/// `end(id:...)`).
@available(iOS 16.2, *)
public final class LiveActivityKit {
    public static let shared = LiveActivityKit()

    private let lock = NSLock()
    private var activities: [String: Activity<KmpLiveActivityAttributes>] = [:]
    private var observers: [String: Task<Void, Never>] = [:]
    private var endedByLibrary: Set<String> = []
    private var listener: ((String, String) -> Void)?
    private let decoder = JSONDecoder()
    private let encoder = JSONEncoder()

    public init() {
        for activity in Activity<KmpLiveActivityAttributes>.activities {
            track(activity)
        }
    }

    /// `ActivityAuthorizationInfo().areActivitiesEnabled`.
    public var areActivitiesEnabled: Bool {
        ActivityAuthorizationInfo().areActivitiesEnabled
    }

    /// Encoded records (Kotlin `LiveActivityCodec` format) for activities ActivityKit still tracks.
    public func activeActivities() -> [String] {
        let current = lock.withLock { Array(activities.values) }
        return current.compactMap { activity in
            guard activity.activityState == .active || activity.activityState == .stale else { return nil }
            let record = ActivityRecord(
                id: activity.attributes.id,
                kind: activity.attributes.kind,
                startedAt: nil,
                state: "active",
                deepLink: activity.attributes.deepLink,
                attributes: activity.attributes.attributes,
                content: activity.content.state
            )
            guard let data = try? encoder.encode(record) else { return nil }
            return String(decoding: data, as: UTF8.self)
        }
    }

    /// Requests an activity from an encoded record. `staleAtEpochMillis` of 0 means no stale date.
    public func start(activityJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard let record = try? decoder.decode(ActivityRecord.self, from: Data(activityJson.utf8)) else {
            completion("platform", "Malformed activity record")
            return
        }
        let attributes = KmpLiveActivityAttributes(
            id: record.id,
            kind: record.kind ?? "default",
            attributes: record.attributes ?? [:],
            deepLink: record.deepLink
        )
        let content = ActivityContent(state: record.content, staleDate: Self.staleDate(staleAtEpochMillis))
        do {
            let activity = try Activity.request(attributes: attributes, content: content, pushType: nil)
            track(activity)
            completion(nil, nil)
        } catch {
            let (code, message) = Self.map(error)
            completion(code, message)
        }
    }

    /// Pushes new content to a running activity.
    public func update(id: String, contentJson: String, staleAtEpochMillis: Int64, completion: @escaping (String?, String?) -> Void) {
        guard let activity = lock.withLock({ activities[id] }) else {
            completion("not_found", "No live activity with id \(id)")
            return
        }
        guard let state = try? decoder.decode(KmpLiveActivityAttributes.ContentState.self, from: Data(contentJson.utf8)) else {
            completion("platform", "Malformed content")
            return
        }
        let content = ActivityContent(state: state, staleDate: Self.staleDate(staleAtEpochMillis))
        Task {
            await activity.update(content)
            completion(nil, nil)
        }
    }

    /// Ends an activity. `dismissal` is `default`, `immediate` or `after`.
    public func end(
        id: String,
        finalContentJson: String?,
        dismissal: String,
        dismissAtEpochMillis: Int64,
        completion: @escaping (String?, String?) -> Void
    ) {
        guard let activity = lock.withLock({ activities[id] }) else {
            completion("not_found", "No live activity with id \(id)")
            return
        }
        let finalState = finalContentJson.flatMap { json in
            try? decoder.decode(KmpLiveActivityAttributes.ContentState.self, from: Data(json.utf8))
        }
        let policy: ActivityUIDismissalPolicy
        switch dismissal {
        case "immediate": policy = .immediate
        case "after": policy = .after(Date(epochMillis: dismissAtEpochMillis))
        default: policy = .default
        }
        lock.withLock { _ = endedByLibrary.insert(id) }
        Task {
            await activity.end(finalState.map { ActivityContent(state: $0, staleDate: nil) }, dismissalPolicy: policy)
            completion(nil, nil)
        }
    }

    /// Installs the single `(id, state)` listener.
    public func setStateListener(_ listener: @escaping (String, String) -> Void) {
        lock.withLock { self.listener = listener }
    }

    private func track(_ activity: Activity<KmpLiveActivityAttributes>) {
        let id = activity.attributes.id
        let previous: Task<Void, Never>? = lock.withLock {
            activities[id] = activity
            return observers[id]
        }
        previous?.cancel()
        let observer = Task { [weak self] in
            for await state in activity.activityStateUpdates {
                guard let self else { return }
                self.emit(id: id, state: state)
                if state == .dismissed { self.forget(id) }
            }
        }
        lock.withLock { observers[id] = observer }
    }

    private func emit(id: String, state: ActivityState) {
        let (listener, endedByUs) = lock.withLock { (self.listener, endedByLibrary.contains(id)) }
        let name: String
        switch state {
        case .active: name = "active"
        case .ended: name = endedByUs ? "ended" : "expired"
        case .dismissed: name = endedByUs ? "dismissed" : "expired"
        default: return
        }
        listener?(id, name)
    }

    private func forget(_ id: String) {
        let observer: Task<Void, Never>? = lock.withLock {
            activities[id] = nil
            endedByLibrary.remove(id)
            return observers.removeValue(forKey: id)
        }
        observer?.cancel()
    }

    private static func staleDate(_ epochMillis: Int64) -> Date? {
        epochMillis > 0 ? Date(epochMillis: epochMillis) : nil
    }

    private static func map(_ error: Error) -> (String, String) {
        if let authError = error as? ActivityAuthorizationError {
            switch authError {
            case .attributesTooLarge: return ("too_large", authError.localizedDescription)
            case .targetMaximumExceeded, .globalMaximumExceeded: return ("too_many", authError.localizedDescription)
            case .denied: return ("denied", authError.localizedDescription)
            case .unsupported, .unsupportedTarget: return ("unsupported", authError.localizedDescription)
            default: return ("platform", authError.localizedDescription)
            }
        }
        return ("platform", error.localizedDescription)
    }
}

/// Wire record shared with the Kotlin `LiveActivityCodec`.
@available(iOS 16.2, *)
struct ActivityRecord: Codable {
    var id: String
    var kind: String?
    var startedAt: Int64?
    var state: String?
    var deepLink: String?
    var attributes: [String: String]?
    var content: KmpLiveActivityAttributes.ContentState
}
