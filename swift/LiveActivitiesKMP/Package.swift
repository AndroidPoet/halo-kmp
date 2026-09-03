// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LiveActivitiesKMP",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "LiveActivitiesKMP", targets: ["LiveActivitiesKMP"]),
    ],
    targets: [
        .target(name: "LiveActivitiesKMP"),
    ]
)
