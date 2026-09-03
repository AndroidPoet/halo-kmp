import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Registers the ActivityKit bridge before any Compose screen asks for a manager.
        Halo.shared.register(bridge: HaloBridge())
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}
