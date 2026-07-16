import SwiftUI
import ComposeApp
import FirebaseAppCheck
import FirebaseCore

private final class PlzStopAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        if #available(iOS 14.0, *) {
            return AppAttestProvider(app: app)
        }
        return DeviceCheckProvider(app: app)
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
#if DEBUG
        AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
#else
        AppCheck.setAppCheckProviderFactory(PlzStopAppCheckProviderFactory())
#endif
        FirebaseApp.configure()
        return true
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) private var scenePhase

    private let lifecycleHandler: IosAppLifecycleHandler
    private let deepLinkHandler = DeepLinkHandler(resolver: DeepLinkResolver())

    init() {
        KermitInitializeUtilsKt.doInitLogger(minSeverity: .verbose)

        let platformOverrides = IosKoinHelperKt.createIosPlatformOverrides(
            firebaseFunctionsCaller: AppFirebaseFunctionsCaller(),
            fcmTokenBridge: AppFcmTokenBridge(),
            firebaseAuthBridge: AppFirebaseAuthBridge(),
            socialAuthBridge: AppSocialAuthBridge()
        )
        KoinApplication.start(platformOverrides: platformOverrides)
        lifecycleHandler = KoinApplication.inject()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(deepLinkHandler: deepLinkHandler)
        }
        .onChange(of: scenePhase) { _, newPhase in
            switch newPhase {
            case .active:
                lifecycleHandler.onAppBecameActive()
            case .background:
                lifecycleHandler.onAppEnteredBackground()
            default:
                break
            }
        }
    }
}
