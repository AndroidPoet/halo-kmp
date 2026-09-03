import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Registers the ActivityKit bridge before any Compose screen asks for a manager.
        LiveActivities.shared.register(bridge: LiveActivitiesBridge())
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}
