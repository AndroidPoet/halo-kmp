import ActivityKit
import Foundation

/// The single `ActivityAttributes` type shared by the app and its widget
/// extension. ActivityKit matches an activity to its widget by this type, so
/// both targets must link this package; never copy or re-declare it.
///
/// `ContentState` mirrors the Kotlin `LiveActivityContent` JSON key for key.
/// Every field is optional so a payload written by one app version decodes on
/// another while an activity is still running.
@available(iOS 16.2, *)
public struct KmpLiveActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var title: String?
        public var subtitle: String?
        public var shortText: String?
        public var progressCurrent: Int?
        public var progressMax: Int?
        public var progressIndeterminate: Bool?
        public var timerStartAt: Int64?
        public var timerEndAt: Int64?
        public var timerCountsDown: Bool?
        public var timerPausedAt: Int64?
        public var icon: String?
        public var accentColorArgb: Int64?
        public var values: [String: String]?

        public init(
            title: String? = nil,
            subtitle: String? = nil,
            shortText: String? = nil,
            progressCurrent: Int? = nil,
            progressMax: Int? = nil,
            progressIndeterminate: Bool? = nil,
            timerStartAt: Int64? = nil,
            timerEndAt: Int64? = nil,
            timerCountsDown: Bool? = nil,
            timerPausedAt: Int64? = nil,
            icon: String? = nil,
            accentColorArgb: Int64? = nil,
            values: [String: String]? = nil
        ) {
            self.title = title
            self.subtitle = subtitle
            self.shortText = shortText
            self.progressCurrent = progressCurrent
            self.progressMax = progressMax
            self.progressIndeterminate = progressIndeterminate
            self.timerStartAt = timerStartAt
            self.timerEndAt = timerEndAt
            self.timerCountsDown = timerCountsDown
            self.timerPausedAt = timerPausedAt
            self.icon = icon
            self.accentColorArgb = accentColorArgb
            self.values = values
        }

        /// Timer bounds as dates, or nil when the content has no timer.
        public var timerRange: ClosedRange<Date>? {
            guard let start = timerStartAt, let end = timerEndAt, start <= end else { return nil }
            return Date(epochMillis: start) ... Date(epochMillis: end)
        }

        /// Instant the timer is frozen at, if paused.
        public var pauseDate: Date? { timerPausedAt.map(Date.init(epochMillis:)) }

        /// Determinate progress as a 0...1 fraction, or nil.
        public var progressFraction: Double? {
            guard let current = progressCurrent, let max = progressMax, max > 0 else { return nil }
            return min(1, Swift.max(0, Double(current) / Double(max)))
        }
    }

    public var id: String
    public var kind: String
    public var attributes: [String: String]
    public var deepLink: String?

    public init(id: String, kind: String = "default", attributes: [String: String] = [:], deepLink: String? = nil) {
        self.id = id
        self.kind = kind
        self.attributes = attributes
        self.deepLink = deepLink
    }
}

extension Date {
    /// Builds a date from milliseconds since 1970, the unit the Kotlin side uses.
    public init(epochMillis: Int64) {
        self.init(timeIntervalSince1970: TimeInterval(epochMillis) / 1000)
    }

    /// Milliseconds since 1970.
    public var epochMillis: Int64 { Int64((timeIntervalSince1970 * 1000).rounded()) }
}
