import Foundation
import ComposeApp

typealias KoinApplication = Koin_coreKoinApplication
typealias Koin = Koin_coreKoin

extension KoinApplication {

    static var shared: KoinApplication!

    private static let keyPaths: [PartialKeyPath<Koin>] = [
        \.appLifecycleHandler,
    ]

    static func start(platformOverrides: Koin_coreModule) {
        shared = KoinKt.doInitKoin(config: nil, platformOverrides: platformOverrides)
    }

    static func inject<T>() -> T {
        shared.inject()
    }

    func inject<T>() -> T {
        for partialKeyPath in Self.keyPaths {
            guard let keyPath = partialKeyPath as? KeyPath<Koin, T> else { continue }
            return koin[keyPath: keyPath]
        }
        fatalError("\(T.self) is not registered with KoinApplication")
    }
}
