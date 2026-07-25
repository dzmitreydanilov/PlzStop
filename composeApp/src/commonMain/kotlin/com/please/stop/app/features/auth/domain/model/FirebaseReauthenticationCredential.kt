package com.please.stop.app.features.auth.domain.model

import kotlin.jvm.JvmInline

/** Fresh provider credential used to confirm ownership of the current Firebase account. */
sealed interface FirebaseReauthenticationCredential {

    /** Google ID token for a Firebase account authenticated with Google. */
    @JvmInline
    value class Google(val idToken: String) : FirebaseReauthenticationCredential

    /** Apple identity token and matching raw nonce for a Firebase account authenticated with Apple. */
    data class Apple(
        val identityToken: String,
        val nonce: String,
    ) : FirebaseReauthenticationCredential
}
