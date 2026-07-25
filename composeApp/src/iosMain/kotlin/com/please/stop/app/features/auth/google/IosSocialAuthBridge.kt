package com.please.stop.app.features.auth.google

import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("IosSocialAuthBridge", exact = true)
interface IosSocialAuthBridge {

    fun signInWithGoogle(
        onSuccess: (idToken: String) -> Unit,
        onError: (String) -> Unit,
    )

    fun authorizeGoogleSheets(
        scopes: List<String>,
        forceConsent: Boolean,
        onSuccess: (serverAuthCode: String) -> Unit,
        onError: (String) -> Unit,
    )

    fun signInWithApple(
        onSuccess: (identityToken: String, nonce: String, email: String?) -> Unit,
        onError: (String) -> Unit,
    )

    fun cancelAppleSignIn()

    fun signOut(onComplete: () -> Unit)
}
