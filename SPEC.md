# halo-kmp — Specification (v0.1.0, post-critique)

One common Kotlin Multiplatform API to **start, update and end a live activity** —
a system-surfaced, glanceable, ongoing status card — backed by **ActivityKit**
(Lock Screen + Dynamic Island) on iOS and **Android 16 Live Updates**
(promoted ongoing notification with `ProgressStyle` / chronometer) on Android,
with an ongoing-notification fallback on older Android.

Repo `AndroidPoet/halo-kmp`. Artifacts `io.github.androidpoet:halo`,
`io.github.androidpoet:halo-compose`. Package `io.github.androidpoet.halo`.
Built on the passkeys-kmp house standard (explicitApi, BCV dumps, detekt, spotless/ktlint,
kover, dokka, KDoc everywhere, CoC, Nextra docs, vanniktech publish).

## 1. Platform facts the design is built on

| | iOS ActivityKit | Android Live Updates |
|---|---|---|
| Rendering | SwiftUI in a **widget extension** (Swift only) | System templates: `ProgressStyle`, chronometer, short critical text |
| Identity | `Activity<Attributes>`; matched to the widget by the **attributes type** | Notification `(tag = activityId, id = 0)` |
| Ticking time | `Text(timerInterval: start...end, pauseTime:, countsDown:)` | `setUsesChronometer` + `setChronometerCountDown` from `setWhen` |
| Authorization | `NSSupportsLiveActivities` + `ActivityAuthorizationInfo.areActivitiesEnabled` | `POST_NOTIFICATIONS` (33+); promotion additionally needs `canPostPromotedNotifications()` |
| Promotion | n/a | Requested via `setRequestPromotedOngoing(true)` (compat extra on 36.0, platform API 36.1); system decides, no error on refusal |
| Max duration | **8 h active**, then system-ended; up to 4 h more on Lock Screen | Unbounded while ongoing |
| Payload | ~4 KB attributes + state | Extras cross Binder; keep small |
| End | `end(content, dismissalPolicy:)` | `cancel()` or repost non-ongoing final card with `setTimeoutAfter` (best effort, some OEMs ignore) |
| Below floor | iOS < 16.2 → `Unsupported` | API < 36 → plain ongoing notification, no chip; API 26 floor |

Design constraints: the library never needs per-second updates; the library never
auto-restarts an activity the system expired; `kind` is grouping metadata only and
selects no UI on either platform.

## 2. Public API (commonMain)

```kotlin
public interface LiveActivityManager {
    public val isSupported: Boolean
    public suspend fun authorization(): LiveActivityAuthorization
    public val activities: StateFlow<List<LiveActivity>>
    public suspend fun start(request: LiveActivityRequest): LiveActivityResult<LiveActivity>
    public suspend fun update(id: String, content: LiveActivityContent): LiveActivityResult<Unit>
    public suspend fun end(id: String, finalContent: LiveActivityContent? = null,
                           dismissal: LiveActivityDismissal = LiveActivityDismissal.Default): LiveActivityResult<Unit>
    public suspend fun endAll(dismissal: LiveActivityDismissal = LiveActivityDismissal.Immediate): LiveActivityResult<Unit>
    /** Stops observing platform state. Compose helper calls this from DisposableEffect. */
    public fun close()
}

public enum class LiveActivityAuthorization { Authorized, Denied, Unsupported }

public data class LiveActivityRequest(
    val content: LiveActivityContent,
    val id: String = generateLiveActivityId(),
    val kind: String = "default",
    val attributes: Map<String, String> = emptyMap(),
    val deepLink: String? = null,
)

public data class LiveActivityContent(
    val title: String,
    val subtitle: String? = null,
    /** Status-bar chip / Dynamic Island compact text. Replaces the ticking timer in that slot; null lets the timer show. */
    val shortText: String? = null,
    val progress: LiveActivityProgress? = null,
    val timer: LiveActivityTimer? = null,
    /** iOS: SF Symbol name. Android: key passed to AndroidLiveActivityConfig.iconResolver (never getIdentifier). */
    val icon: String? = null,
    val accentColorArgb: Long? = null,
    /** iOS: ActivityContent.staleDate (not part of ContentState). Android: ignored. */
    val staleAtEpochMillis: Long? = null,
    val values: Map<String, String> = emptyMap(),
)

public sealed interface LiveActivityProgress {
    public data class Determinate(val current: Int, val max: Int) : LiveActivityProgress
    public data object Indeterminate : LiveActivityProgress
}

/** Full interval so both platforms render natively; iOS needs a ClosedRange<Date>. */
public data class LiveActivityTimer(
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val countsDown: Boolean = true,
    val pausedAtEpochMillis: Long? = null,
)

public sealed interface LiveActivityDismissal {
    /** iOS: system default (≤ 4 h on Lock Screen). Android: final card stays as a normal, swipeable notification, timeout 4 h (best effort). */
    public data object Default : LiveActivityDismissal
    public data object Immediate : LiveActivityDismissal
    public data class After(val epochMillis: Long) : LiveActivityDismissal
}

public data class LiveActivity(val id: String, val kind: String, val content: LiveActivityContent,
                               val state: LiveActivityState, val startedAtEpochMillis: Long,
                               val attributes: Map<String, String> = emptyMap(), val deepLink: String? = null)

/** Expired = ended by the system (iOS 8 h ceiling), not by this library. */
public enum class LiveActivityState { Active, Ended, Dismissed, Expired }

public sealed class LiveActivityResult<out T> {
    public data class Success<T>(val value: T) : LiveActivityResult<T>()
    public data class Failure(val error: LiveActivityException) : LiveActivityResult<Nothing>()
}

public sealed class LiveActivityException(code: Int, message: String, cause: Throwable?) : Exception(message, cause) {
    Unsupported(2001), NotAuthorized(2002), NotFound(2003), InvalidContent(2004, message),
    TooManyActivities(2005), PayloadTooLarge(2006), AlreadyActive(2007), PlatformError(2008, message)
}
```

Also public in `commonMain`:
- `LiveActivityCodec` — `encodeContent/decodeContent`, `encodeRequest/decodeRequest`, `encodeActivity/decodeActivity`
  (flat JSON, every field optional except `title`, unknown keys ignored). This JSON is the wire contract to Swift and to
  Android notification extras.
- `generateLiveActivityId()` — 32 lowercase hex chars.
- `LiveActivityRequest.validate(): LiveActivityException?` / `LiveActivityContent.validate()`:
  blank title; `Determinate` with `max <= 0`, `current < 0`, `current > max`; timer `end <= start`,
  `pausedAt` outside `[start, end]`; encoded request > 3072 bytes → `PayloadTooLarge`.
- `LiveActivityDismissal.After` in the past → `InvalidContent`.
- `UnsupportedLiveActivityManager` (public, every target) — `isSupported=false`, every call `Failure(Unsupported)`.

### Compose module
`@Composable public expect fun rememberLiveActivityManager(): LiveActivityManager` — remembered, closed in
`DisposableEffect`. Android → `AndroidLiveActivityManager(LocalContext.applicationContext, Halo.androidConfig)`;
iOS → `IosLiveActivityManager()`; jvm/wasmJs → `UnsupportedLiveActivityManager`.

## 3. Platform behaviour

### 3.1 Android (minSdk 26, compileSdk 36, androidx.core 1.17.0 — compat APIs only, no direct API-36.1 calls)

```kotlin
public class AndroidLiveActivityConfig(
    val channelId: String = "live_activities", val channelName: String = "Live activities",
    val channelImportance: Int = IMPORTANCE_DEFAULT,      // applied on first creation only (channels are immutable)
    @DrawableRes val smallIconRes: Int = 0,               // 0 → application icon
    val iconResolver: ((String) -> Int)? = null,          // content.icon → @DrawableRes; null → smallIconRes
    val contentIntent: ((LiveActivity) -> PendingIntent)? = null,
)
public object Halo { public var androidConfig: AndroidLiveActivityConfig }
public class AndroidLiveActivityManager(context: Context, config: AndroidLiveActivityConfig = Halo.androidConfig) {
    public val canPromote: Boolean          // SDK_INT >= 36 && NotificationManagerCompat.canPostPromotedNotifications()
    public fun isPromotable(id: String): Boolean  // SDK_INT >= 36 && notification.hasPromotableCharacteristics(), else false
}
```
- Stores `context.applicationContext`; creates the channel in `init` (idempotent).
- Builder mapping: `setOngoing(true)`, `setOnlyAlertOnce(true)`, `setSilent(true)`, `setRequestPromotedOngoing(true)`,
  `setContentTitle(title)`, `setContentText(subtitle)`, `setShortCriticalText(shortText)` (only when non-null),
  `setSmallIcon(resolved)`, `setColor(accent)` when given, `setContentIntent` (config factory, else `ACTION_VIEW deepLink`, immutable).
  `Determinate` → `ProgressStyle().setProgress(current).setProgressSegments(listOf(Segment(max)))`;
  `Indeterminate` → `ProgressStyle().setProgressIndeterminate(true)`; no progress → no style
  (title+text standard notifications are promotable; verified empirically in A5).
  Timer: not paused → `setWhen(countsDown ? end : start).setUsesChronometer(true).setChronometerCountDown(countsDown).setShowWhen(true)`;
  paused → `setUsesChronometer(false)`, `setSubText("Paused")`.
  Extras: `EXTRA_LIVE_ACTIVITY` = encoded activity record; identity `tag = activityId, id = 0`.
- `start` existing id → `AlreadyActive`; `update`/`end` unknown → `NotFound`.
- `end`: `Immediate` or `finalContent == null` → cancel, state `Dismissed`. Else repost non-ongoing final card,
  `setTimeoutAfter(Default: 4 h, After: max(0, t - now))`, state `Ended`.
- `authorization()`: `Denied` when (33+ and `POST_NOTIFICATIONS` not granted) or `!areNotificationsEnabled()` or channel importance NONE; else `Authorized`.
- Restore (process death only, not reboot): `activeNotifications` whose extras contain `EXTRA_LIVE_ACTIVITY` are decoded into `activities`.

### 3.2 iOS (deployment 16.2; ActivityKit is Swift-only)

Kotlin `iosMain`:
```kotlin
public interface LiveActivityBridge {
    public fun areActivitiesEnabled(): Boolean
    public fun activeActivities(): List<String>                       // encoded activity records (full content restored)
    public fun start(activityJson: String, staleAtEpochMillis: Long, completion: (code: String?, message: String?) -> Unit)
    public fun update(id: String, contentJson: String, staleAtEpochMillis: Long, completion: (String?, String?) -> Unit)
    public fun end(id: String, finalContentJson: String?, dismissal: String, dismissAtEpochMillis: Long, completion: (String?, String?) -> Unit)
    public fun setStateListener(listener: (id: String, state: String) -> Unit)  // called once by Halo.register
}
public object Halo {
    public fun register(bridge: LiveActivityBridge)   // installs the single state listener → internal SharedFlow fan-out
    public val bridge: LiveActivityBridge?
}
public class IosLiveActivityManager : LiveActivityManager   // reads Halo.bridge lazily on every call
```
Error code vocabulary from Swift: `unsupported | denied | too_many | too_large | not_found | platform`.
State vocabulary: `active | ended | dismissed | expired` (Swift emits `expired` when `activityStateUpdates` reports
`.ended`/`.dismissed` for an id the library did not end itself).
`isSupported` = iOS ≥ 16.2 && bridge registered (evaluated per read, so late registration recovers).

Swift package `swift/HaloKMP` (no Kotlin dependency; `platforms: [.iOS(.v16)]`; **linked by app and widget extension** —
the single source of `HaloActivityAttributes`, consumers never copy it):
- `HaloActivityAttributes: ActivityAttributes` — static `id, kind, attributes, deepLink`; `ContentState` = every content
  field optional with defaults (schema drift safe), **excluding** `staleAt`, which goes to `ActivityContent(staleDate:)`.
- `HaloKit` (`@MainActor`) — start/update/end/activeActivities over ActivityKit; maps
  `ActivityAuthorizationError` cases to the code vocabulary; restores `Activity.activities` on init; observes
  `activityStateUpdates` per activity and emits state strings.
- `HaloActivityWidget: Widget` — default Lock Screen + Dynamic Island UI. Timer rendered with
  `Text(timerInterval: start...end, pauseTime: pausedAt, countsDown:)` where `start <= end` is guaranteed by validation
  (an elapsed countdown shows 0:00, never traps). `shortText` displaces the timer in compact trailing.
  `widgetURL(deepLink)`. Stale state renders dimmed.
- Glue file `swift/HaloBridge.swift` — the only copied source: `final class HaloBridge: LiveActivityBridge`
  forwarding to `HaloKit`, plus `Halo.shared.register(bridge:)`. Declares no attributes type.
- Link-once rule: the Kotlin framework containing this library must be linked exactly once (the app's umbrella framework);
  the widget extension links only the Swift package.

### 3.3 jvm / macOS / wasmJs → `UnsupportedLiveActivityManager`.

## 4. Acceptance criteria

A1 `commonTest` codec: round-trip title-only, full content, each progress/timer variant, activity record; decode with unknown
   keys and missing optionals; `validate()` rejects blank title, bad progress bounds, `end <= start`, paused outside range,
   > 3072-byte request (`PayloadTooLarge`).
A2 `commonTest` `generateLiveActivityId()` 32 lowercase hex, 1000 unique.
A3 `iosTest` with a fake bridge: unregistered → `Unsupported`; start → 1 Active; update replaces content; end → Ended;
   unknown id → NotFound; each bridge error code → its exception; `expired` state event → `Expired`; restore decodes content.
A4 `androidUnitTest` (pure): `timeoutMillisFor(Default)=4h`, `After` delta clamped ≥ 0; extras round-trip via codec;
   `iconResolver == null` → `smallIconRes`.
A5 Android 16 emulator (36.0 available; 36.1 if installable): (a) Start with `shortText=null` + countdown → promoted
   ongoing card, ticking chip; (b) +5 min updates silently; (c) Finish → non-ongoing final card; (d) kill process, relaunch,
   restored title equals pre-kill title, Finish still removes it; (e) `isPromotable(id)` true. Screenshots.
A6 iPhone 17 simulator: (a) Start → Lock Screen activity (`xcrun simctl io booted screenshot` after lock),
   (b) Dynamic Island expanded/compact, (c) +5 min updates, (d) Finish ends it. `Activity.activities.count == 1` logged from app.
A7 Gates: `./gradlew spotlessCheck detekt apiCheck jvmTest testDebugUnitTest iosSimulatorArm64Test koverVerify` and
   `compileKotlinIosArm64 compileKotlinMacosArm64 compileKotlinJvm compileKotlinWasmJs`;
   Swift: `xcodebuild -scheme HaloKMP -destination 'generic/platform=iOS Simulator' build` in `swift/HaloKMP`.
A8 KDoc on every public declaration; README (one API, platform table, one-time setup, 8 h ceiling, troubleshooting
   "starts but nothing renders"); CHANGELOG 0.1.0; Nextra docs; CoC.

## 5. Out of scope (v0.1)
Push-token remote updates; custom Android RemoteViews; watchOS/macOS; `relevanceScore` (fixed default); per-second updates.

## 6. Sample
`sample/composeApp`, one `commonMain` `App()`: Focus session — Start (25 min countdown, `Determinate(0, 25)`, `shortText = null`),
+5 min (new `end`, `max`), Pause/Resume (`pausedAt`), Finish (final "Session complete", `Default`). Authorization banner;
Android requests `POST_NOTIFICATIONS`. iOS via xcodegen `project.yml`: app target + `iosAppWidgets` (`type: app-extension`,
`NSExtensionPointIdentifier = com.apple.widgetkit-extension`, bundle id child of app, embedded, deployment 16.2) both depending
on the local Swift package; app `Info.plist` `NSSupportsLiveActivities = YES`.

## 7. Critique disposition (2026-09-04)
Accepted and folded in: #1 8 h ceiling + `Expired`; #2 full-interval timer + pause; #3 promotability check (`isPromotable`) with
empirical A5, no forced indeterminate bar; #4 compat-only calls, 36.0 emulator acknowledged; #5 `shortText` precedence; #6
`iconResolver`; #7 coded bridge errors + 3072-byte cap; #8 lazy bridge read, `close()`, DisposableEffect, link-once rule; #9
attributes from one module, all-optional ContentState; #10 `staleDate` on `ActivityContent`; #11 full restore records;
#12 `kind` grouping-only, single channel, `endAll` returns result, `tag = id`; #13 exception hierarchy mirrors passkeys; #14
xcodebuild gate, extension wiring, simctl evidence; channel creation + applicationContext.
Adjusted: #8 listener add/remove → one listener set at `register`, fan-out via SharedFlow (Swift blocks have no stable identity
for removal). Rejected: #12 "60 s Android Default" — a swipeable completion notification is normal Android UX; 4 h best-effort
timeout kept and documented.
