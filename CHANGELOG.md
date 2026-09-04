# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Fixed

- iOS: ending or updating an activity that ActivityKit has already removed now
  fails with `LiveActivityException.NotFound` and marks the activity `Expired`,
  instead of surfacing a `PlatformError`.

## 0.1.0 - 2026-09-04

### Added

- **`halo`** — `LiveActivityManager` with `start`, `update`, `end`, `endAll`,
  `authorization` and an `activities` state flow; typed `LiveActivityResult` /
  `LiveActivityException` (codes 2001–2008); `LiveActivityContent` with progress, a
  natively ticking `LiveActivityTimer` (countdown, count-up, pause), icon, accent and
  free-form values; JSON wire codec shared with Swift and with Android extras.
- **Android** — `AndroidLiveActivityManager`: Android 16 Live Updates (promoted ongoing
  notification with `ProgressStyle`, chronometer and short critical text), ongoing
  notification fallback down to API 26, restore after process death, `canPromote` /
  `isPromotable`.
- **iOS** — `IosLiveActivityManager` over a `LiveActivityBridge`; Swift package
  `HaloKMP` with `HaloActivityAttributes`, `HaloKit` and a default
  `HaloActivityWidget` (Lock Screen + Dynamic Island); system-expired activities surface
  as `Expired`.
- **`halo-compose`** — `rememberLiveActivityManager()` for Compose Multiplatform.
- JVM, macOS and Wasm stubs so shared code compiles everywhere.
