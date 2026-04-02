import SwiftUI
import ComposeApp
import FirebaseCore

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        KermitInitializeUtilsKt.doInitLogger(minSeverity: .verbose)

        let platformOverrides = IosKoinHelperKt.createIosPlatformOverrides(
            firebaseFunctionsCaller: AppFirebaseFunctionsCaller()
        )
        KoinKt.doInitKoin(config: nil, platformOverrides: platformOverrides)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
