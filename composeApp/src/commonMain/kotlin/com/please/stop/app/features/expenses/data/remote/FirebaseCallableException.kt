package com.please.stop.app.features.expenses.data.remote

import kotlin.jvm.JvmInline

/** Stable machine-readable callable failure reason. */
@JvmInline
value class FirebaseCallableErrorReason(val value: String) {
    companion object {
        val FirebaseSignInRequired = FirebaseCallableErrorReason("FIREBASE_SIGN_IN_REQUIRED")
        val GoogleAuthCodeMissing = FirebaseCallableErrorReason("GOOGLE_AUTH_CODE_MISSING")
        val GoogleRefreshTokenMissing = FirebaseCallableErrorReason("GOOGLE_REFRESH_TOKEN_MISSING")
        val GoogleReconnectRequired = FirebaseCallableErrorReason("GOOGLE_RECONNECT_REQUIRED")
        val GoogleScopesMissing = FirebaseCallableErrorReason("GOOGLE_SCOPES_MISSING")
        val GoogleTokenEndpointUnavailable = FirebaseCallableErrorReason("GOOGLE_TOKEN_ENDPOINT_UNAVAILABLE")
        val ExportInProgress = FirebaseCallableErrorReason("EXPORT_IN_PROGRESS")
        val SheetsTemporarilyUnavailable = FirebaseCallableErrorReason("SHEETS_TEMPORARILY_UNAVAILABLE")
    }
}

/** Callable failure with transport code and optional stable recovery reason. */
class FirebaseCallableException(
    val code: String?,
    val reason: FirebaseCallableErrorReason?,
    message: String?,
) : Exception(message)
