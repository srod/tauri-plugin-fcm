// swift-tools-version:5.7
import PackageDescription

let package = Package(
    name: "tauri-plugin-fcm",
    platforms: [
        .macOS(.v10_15),
        .iOS(.v14),
    ],
    products: [
        .library(name: "tauri-plugin-fcm", type: .static, targets: ["tauri-plugin-fcm"])
    ],
    dependencies: [
        .package(name: "Tauri", path: "../.tauri/tauri-api"),
        .package(url: "https://github.com/firebase/firebase-ios-sdk", from: "12.13.0")
    ],
    targets: [
        .target(
            name: "tauri-plugin-fcm",
            dependencies: [
                .byName(name: "Tauri"),
                .product(name: "FirebaseMessaging", package: "firebase-ios-sdk")
            ],
            path: "Sources"
        )
    ]
)
