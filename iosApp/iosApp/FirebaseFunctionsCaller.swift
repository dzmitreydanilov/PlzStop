import FirebaseFunctions
import ComposeApp

class AppFirebaseFunctionsCaller: IosFirebaseFunctionsCaller {

    private lazy var functions = Functions.functions(region: "europe-west1")

    func callFunction(
        name: String,
        data: [String: Any],
        onSuccess: @escaping ([String: Any]) -> Void,
        onError: @escaping (String, String?, String?) -> Void
    ) {
        let callable = functions.httpsCallable(name)

        callable.call(data) { result, error in
            if let error = error {
                let nsError = error as NSError
                let details = nsError.userInfo[FunctionsErrorDetailsKey] as? [String: Any]
                onError(
                    error.localizedDescription,
                    String(nsError.code),
                    details?["reason"] as? String
                )
                return
            }

            guard let resultData = result?.data as? [String: Any] else {
                onError("Unexpected response format", nil, nil)
                return
            }

            onSuccess(resultData)
        }
    }
}
