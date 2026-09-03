// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "HaloKMP",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "HaloKMP", targets: ["HaloKMP"]),
    ],
    targets: [
        .target(name: "HaloKMP"),
    ]
)
