import ComposeApp
import FirebaseAuth

final class AppFirebaseAuthBridge: IosFirebaseAuthBridge {
    private var authStateHandle: AuthStateDidChangeListenerHandle?

    func signInWithGoogle(
        idToken: String,
        onSuccess: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        let credential = OAuthProvider.credential(providerID: .google, idToken: idToken)
        signIn(credential: credential, onSuccess: onSuccess, onError: onError)
    }

    func signInWithApple(
        identityToken: String,
        nonce: String,
        onSuccess: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        let credential = OAuthProvider.appleCredential(
            withIDToken: identityToken,
            rawNonce: nonce,
            fullName: nil
        )
        signIn(credential: credential, onSuccess: onSuccess, onError: onError)
    }

    func deleteAccount(
        onSuccess: @escaping () -> Void,
        onNeedsReauthentication: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let user = Auth.auth().currentUser else {
            onError("No authenticated Firebase user")
            return
        }
        user.delete { error in
            guard let error = error as NSError? else {
                onSuccess()
                return
            }
            if AuthErrorCode(rawValue: error.code) == .requiresRecentLogin {
                onNeedsReauthentication()
            } else {
                onError("Firebase account deletion failed")
            }
        }
    }

    func reauthenticateWithGoogle(
        idToken: String,
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        let credential = OAuthProvider.credential(providerID: .google, idToken: idToken)
        reauthenticate(credential: credential, onSuccess: onSuccess, onError: onError)
    }

    func reauthenticateWithApple(
        identityToken: String,
        nonce: String,
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        let credential = OAuthProvider.appleCredential(
            withIDToken: identityToken,
            rawNonce: nonce,
            fullName: nil
        )
        reauthenticate(credential: credential, onSuccess: onSuccess, onError: onError)
    }

    func signOut(onComplete: @escaping () -> Void) {
        try? Auth.auth().signOut()
        onComplete()
    }

    func currentSignInProviderId() -> String? {
        return Auth.auth().currentUser?.providerData.first(where: { userInfo in
            userInfo.providerID == AuthProviderID.google.rawValue ||
                userInfo.providerID == AuthProviderID.apple.rawValue
        })?.providerID
    }

    func observeIsAuthenticated(onChanged: @escaping (KotlinBoolean) -> Void) {
        removeAuthStateListener()
        authStateHandle = Auth.auth().addStateDidChangeListener { _, user in
            onChanged(KotlinBoolean(value: user != nil))
        }
    }

    func removeAuthStateListener() {
        guard let authStateHandle else { return }
        Auth.auth().removeStateDidChangeListener(authStateHandle)
        self.authStateHandle = nil
    }

    private func signIn(
        credential: AuthCredential,
        onSuccess: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        Auth.auth().signIn(with: credential) { result, error in
            if error != nil {
                onError("Firebase sign-in failed")
            } else if let uid = result?.user.uid {
                onSuccess(uid)
            } else {
                onError("Firebase user unavailable")
            }
        }
    }

    private func reauthenticate(
        credential: AuthCredential,
        onSuccess: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        guard let user = Auth.auth().currentUser else {
            onError("No authenticated Firebase user")
            return
        }
        user.reauthenticate(with: credential) { _, error in
            if error != nil {
                onError("Firebase reauthentication failed")
            } else {
                onSuccess()
            }
        }
    }
}
