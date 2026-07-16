import AuthenticationServices
import ComposeApp
import CryptoKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import Security
import UIKit

final class AppSocialAuthBridge: NSObject, IosSocialAuthBridge {
    private var appleCompletion: ((String, String, String?) -> Void)?
    private var appleError: ((String) -> Void)?
    private var appleNonce: String?

    func signInWithGoogle(
        onSuccess: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        performGoogleSignIn(
            accountHint: Auth.auth().currentUser?.email,
            additionalScopes: [],
            onError: onError
        ) { result in
            guard let idToken = result.user.idToken?.tokenString else {
                onError("Google identity token unavailable")
                return
            }
            onSuccess(idToken)
        }
    }

    func authorizeGoogleSheets(
        scopes: [String],
        forceConsent: Bool,
        onSuccess: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        let authorize = { [weak self] in
            self?.performGoogleSignIn(
                accountHint: nil,
                additionalScopes: scopes,
                onError: onError
            ) { result in
                guard let serverAuthCode = result.serverAuthCode else {
                    onError("Google server authorization code unavailable")
                    return
                }
                onSuccess(serverAuthCode)
            }
        }

        if forceConsent {
            GIDSignIn.sharedInstance.disconnect { error in
                if error != nil {
                    onError("Google reconnect failed")
                    return
                }
                authorize()
            }
        } else {
            authorize()
        }
    }

    private func performGoogleSignIn(
        accountHint: String?,
        additionalScopes: [String],
        onError: @escaping (String) -> Void,
        onSuccess: @escaping (GIDSignInResult) -> Void
    ) {
        guard configureGoogleSignIn() else {
            onError("Google Sign-In OAuth client IDs are not configured")
            return
        }
        guard let presentingViewController = Self.presentingViewController else {
            onError("Unable to present Google Sign-In")
            return
        }
        GIDSignIn.sharedInstance.signIn(
            withPresenting: presentingViewController,
            hint: accountHint,
            additionalScopes: additionalScopes
        ) { result, error in
            if error != nil {
                onError("Google Sign-In failed")
                return
            }
            guard let result else {
                onError("Google Sign-In result unavailable")
                return
            }
            onSuccess(result)
        }
    }

    func signInWithApple(
        onSuccess: @escaping (String, String, String?) -> Void,
        onError: @escaping (String) -> Void
    ) {
        clearAppleCallbacks()
        let nonce = Self.randomNonce()
        appleNonce = nonce
        appleCompletion = onSuccess
        appleError = onError

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func signOut(onComplete: @escaping () -> Void) {
        GIDSignIn.sharedInstance.signOut()
        onComplete()
    }

    func cancelAppleSignIn() {
        clearAppleCallbacks()
    }

    private func configureGoogleSignIn() -> Bool {
        guard
            let clientID = FirebaseApp.app()?.options.clientID,
            let serverClientID = Bundle.main.object(forInfoDictionaryKey: "GIDServerClientID") as? String
        else {
            return false
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientID,
            serverClientID: serverClientID
        )
        return true
    }

    private static var presentingViewController: UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var controller = scene?.keyWindow?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }

    private static func randomNonce(length: Int = 32) -> String {
        let characters = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        while result.count < length {
            var bytes = [UInt8](repeating: 0, count: 16)
            guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else {
                fatalError("Unable to generate Apple Sign-In nonce")
            }
            result.append(contentsOf: bytes.compactMap { byte in
                byte < characters.count ? characters[Int(byte)] : nil
            })
        }
        return String(result.prefix(length))
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

extension AppSocialAuthBridge: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        guard
            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = credential.identityToken,
            let identityToken = String(data: tokenData, encoding: .utf8),
            let nonce = appleNonce
        else {
            appleError?("Apple identity token unavailable")
            clearAppleCallbacks()
            return
        }
        appleCompletion?(identityToken, nonce, credential.email)
        clearAppleCallbacks()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        appleError?("Apple Sign-In failed")
        clearAppleCallbacks()
    }

    private func clearAppleCallbacks() {
        appleCompletion = nil
        appleError = nil
        appleNonce = nil
    }
}

extension AppSocialAuthBridge: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        Self.presentingViewController?.view.window ?? ASPresentationAnchor()
    }
}
