import ActivityKit
import SwiftUI
import WidgetKit

/// Default Lock Screen and Dynamic Island UI for activities started from Kotlin.
/// Add it to the widget extension's `WidgetBundle`; build your own
/// `ActivityConfiguration(for: HaloActivityAttributes.self)` instead when you
/// want a custom look.
@available(iOS 16.2, *)
public struct HaloActivityWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        ActivityConfiguration(for: HaloActivityAttributes.self) { context in
            KmpLockScreenView(state: context.state, isStale: context.isStale)
                .widgetURL(context.attributes.deepLinkURL)
        } dynamicIsland: { context in
            let state = context.state
            let accent = state.accent
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    KmpIconView(name: state.icon, accent: accent, size: 28)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    KmpTimerText(state: state)
                        .font(.system(.title3, design: .rounded).weight(.semibold).monospacedDigit())
                        .foregroundStyle(accent)
                        .lineLimit(1)
                        .frame(minWidth: 72, alignment: .trailing)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(state.title ?? "").font(.headline)
                        if let subtitle = state.subtitle { Text(subtitle).font(.subheadline).foregroundStyle(.secondary) }
                        KmpProgressView(state: state, accent: accent)
                    }
                }
            } compactLeading: {
                KmpIconView(name: state.icon, accent: accent, size: 16)
            } compactTrailing: {
                if let shortText = state.shortText {
                    Text(shortText).font(.caption2.weight(.semibold)).foregroundStyle(accent)
                } else {
                    KmpTimerText(state: state)
                        .font(.caption2.weight(.semibold).monospacedDigit())
                        .foregroundStyle(accent)
                        .frame(maxWidth: 56)
                }
            } minimal: {
                KmpIconView(name: state.icon, accent: accent, size: 14)
            }
            .widgetURL(context.attributes.deepLinkURL)
        }
    }
}

@available(iOS 16.2, *)
struct KmpLockScreenView: View {
    let state: HaloActivityAttributes.ContentState
    let isStale: Bool

    var body: some View {
        let accent = state.accent
        HStack(spacing: 14) {
            KmpIconView(name: state.icon, accent: accent, size: 34)
            VStack(alignment: .leading, spacing: 4) {
                Text(state.title ?? "").font(.headline).lineLimit(1)
                if let subtitle = state.subtitle {
                    Text(subtitle).font(.subheadline).foregroundStyle(.secondary).lineLimit(1)
                }
                KmpProgressView(state: state, accent: accent)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 2) {
                // A fixed minimum width keeps Text(timerInterval:) from collapsing to "mm:--".
                KmpTimerText(state: state)
                    .font(.system(.title2, design: .rounded).weight(.semibold).monospacedDigit())
                    .foregroundStyle(accent)
                    .lineLimit(1)
                    .frame(minWidth: 92, alignment: .trailing)
                if let shortText = state.shortText {
                    Text(shortText).font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .opacity(isStale ? 0.6 : 1)
        .activityBackgroundTint(Color(.systemBackground).opacity(0.85))
    }
}

@available(iOS 16.2, *)
struct KmpIconView: View {
    let name: String?
    let accent: Color
    let size: CGFloat

    var body: some View {
        Image(systemName: name ?? "circle.fill")
            .font(.system(size: size * 0.6, weight: .semibold))
            .foregroundStyle(accent)
            .frame(width: size, height: size)
            .background(accent.opacity(0.15), in: Circle())
    }
}

@available(iOS 16.2, *)
struct KmpTimerText: View {
    let state: HaloActivityAttributes.ContentState

    var body: some View {
        if let range = state.timerRange {
            Text(timerInterval: range, pauseTime: state.pauseDate, countsDown: state.timerCountsDown ?? true)
                .multilineTextAlignment(.trailing)
        } else {
            EmptyView()
        }
    }
}

@available(iOS 16.2, *)
struct KmpProgressView: View {
    let state: HaloActivityAttributes.ContentState
    let accent: Color

    var body: some View {
        if let fraction = state.progressFraction {
            ProgressView(value: fraction).tint(accent)
        } else if state.progressIndeterminate == true {
            ProgressView().tint(accent)
        } else {
            EmptyView()
        }
    }
}

@available(iOS 16.2, *)
extension HaloActivityAttributes {
    var deepLinkURL: URL? { deepLink.flatMap(URL.init(string:)) }
}

@available(iOS 16.2, *)
extension HaloActivityAttributes.ContentState {
    /// Accent from `accentColorArgb` (0xAARRGGBB), defaulting to the system accent.
    public var accent: Color {
        guard let argb = accentColorArgb else { return .accentColor }
        let alpha = Double((argb >> 24) & 0xFF) / 255
        let red = Double((argb >> 16) & 0xFF) / 255
        let green = Double((argb >> 8) & 0xFF) / 255
        let blue = Double(argb & 0xFF) / 255
        return Color(red: red, green: green, blue: blue, opacity: alpha == 0 ? 1 : alpha)
    }
}
