import ComposeApp
import FirebaseMessaging

class AppFcmTokenBridge: IosFcmTokenBridge {

    func getToken(
        onSuccess: @escaping (String) -> Void,
        onError: @escaping (String) -> Void
    ) {
        Messaging.messaging().token { token, error in
            if let error = error {
                onError(error.localizedDescription)
                return
            }

            guard let token = token else {
                onError("FCM token unavailable")
                return
            }

            onSuccess(token)
        }
    }
}
