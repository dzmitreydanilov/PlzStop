// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/sqlcipher/SQLCipher.swift.git",
      from: "4.14.0"
    ),
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      from: "11.12.0"
    )
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(
          name: "SQLCipher",
          package: "SQLCipher.swift"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseRemoteConfig",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseFunctions",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseAppCheck",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseMessaging",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
