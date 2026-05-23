package com.please.stop.app.features.auth.google

/**
 * Provider class for Google Authentication UI part. a.k.a [signIn]
 */
interface GoogleAuthUiProvider {

    companion object {
        internal val BASIC_AUTH_SCOPE = listOf("email", "profile")
    }

    suspend fun signIn(): GoogleUser? =
        signIn(
            filterByAuthorizedAccounts = false,
            isAutoSelectEnabled = true,
            scopes = BASIC_AUTH_SCOPE
        )

    suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean = true
    ): GoogleUser? =
        signIn(
            filterByAuthorizedAccounts = filterByAuthorizedAccounts,
            isAutoSelectEnabled = isAutoSelectEnabled,
            scopes = BASIC_AUTH_SCOPE
        )

    suspend fun signIn(
        filterByAuthorizedAccounts: Boolean = false,
        isAutoSelectEnabled: Boolean = true,
        scopes: List<String> = BASIC_AUTH_SCOPE
    ): GoogleUser?
}
