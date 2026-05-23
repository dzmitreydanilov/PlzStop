package com.please.stop.app.features.auth.google

import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("IosSocialAuthBridge", exact = true)
interface IosSocialAuthBridge {

    fun signInWithGoogle(
        scopes: List<String>,
        onSuccess: (idToken: String, accessToken: String?) -> Unit,
        onError: (String) -> Unit,
    )

    fun signInWithApple(
        onSuccess: (identityToken: String, nonce: String, email: String?) -> Unit,
        onError: (String) -> Unit,
    )

    fun getGoogleAccessToken(
        scopes: List<String>,
        onSuccess: (accessToken: String) -> Unit,
        onError: (String) -> Unit,
    )

    fun signOut(onComplete: () -> Unit)
}
