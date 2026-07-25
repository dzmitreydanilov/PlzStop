package com.please.stop.app.features.auth.google

/** Foreground Google identity and Sheets authorization operations. */
interface GoogleAuthUiProvider {

    companion object {
        internal val GOOGLE_SHEETS_SCOPES = listOf(
            "https://www.googleapis.com/auth/spreadsheets",
            "https://www.googleapis.com/auth/drive.file",
        )
    }

    /** Requests a Google identity credential for Firebase authentication. */
    suspend fun signIn(): GoogleSignInCredential? =
        signIn(
            filterByAuthorizedAccounts = false,
            isAutoSelectEnabled = false,
        )

    /** Requests a Google identity credential for Firebase authentication. */
    suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean = false,
    ): GoogleSignInCredential?

    /** Requests offline authorization for the exact Google Sheets export scopes. */
    suspend fun authorizeSheets(forceConsent: Boolean = false): GoogleSheetsAuthorizationCode?
}
