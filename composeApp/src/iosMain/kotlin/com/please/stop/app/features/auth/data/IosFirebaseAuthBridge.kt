package com.please.stop.app.features.auth.data

import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("IosFirebaseAuthBridge", exact = true)
interface IosFirebaseAuthBridge {

    fun signInWithGoogle(
        idToken: String,
        onSuccess: (uid: String) -> Unit,
        onError: (String) -> Unit,
    )

    fun signInWithApple(
        identityToken: String,
        nonce: String,
        onSuccess: (uid: String) -> Unit,
        onError: (String) -> Unit,
    )

    fun deleteAccount(
        onSuccess: () -> Unit,
        onNeedsReauthentication: () -> Unit,
        onError: (String) -> Unit,
    )

    fun reauthenticateWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    fun reauthenticateWithApple(
        identityToken: String,
        nonce: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    fun signOut(onComplete: () -> Unit)

    fun currentSignInProviderId(): String?

    fun observeIsAuthenticated(onChanged: (Boolean) -> Unit)

    fun removeAuthStateListener()
}
