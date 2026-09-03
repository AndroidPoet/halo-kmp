import HaloKMP
import SwiftUI
import WidgetKit

/// The whole widget extension: the default Lock Screen + Dynamic Island UI from the package.
@main
struct SampleWidgets: WidgetBundle {
    var body: some Widget {
        HaloActivityWidget()
    }
}
