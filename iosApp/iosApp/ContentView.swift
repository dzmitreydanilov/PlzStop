import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var isComposeReady = false

    var body: some View {
        ZStack {
            ComposeView()
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
    }
}
