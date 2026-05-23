import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let deepLinkHandler: DeepLinkHandler

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            deepLinkUri: nil,
            deepLinkHandler: deepLinkHandler
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let deepLinkHandler: DeepLinkHandler

    @State private var isComposeReady = false

    var body: some View {
        ZStack {
            ComposeView(deepLinkHandler: deepLinkHandler)
                .ignoresSafeArea()
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        withAnimation(.easeOut(duration: 0.3)) {
                            isComposeReady = true
                        }
                    }
                }

            if !isComposeReady {
                SplashScreenView()
                    .transition(.opacity)
            }
        }
        .onOpenURL { url in
            deepLinkHandler.handleDeepLink(uriString: url.absoluteString)
        }
    }
}
