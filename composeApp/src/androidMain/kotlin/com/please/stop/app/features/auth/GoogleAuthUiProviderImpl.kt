package com.please.stop.app.features.auth

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.please.stop.app.core.logger.logDebug
import com.please.stop.app.features.auth.google.GoogleAuthCredentials
import com.please.stop.app.features.auth.google.GoogleAuthUiProvider
import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import com.please.stop.app.features.auth.google.PendingAuthorizationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class GoogleAuthUiProviderImpl(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val credentials: GoogleAuthCredentials,
    private val scopeIntentLauncher: (IntentSenderRequest) -> Unit,
    private val pendingAuthorizationResult: PendingAuthorizationResult<ActivityResult>,
) : GoogleAuthUiProvider {

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
    ): GoogleSignInCredential? {
        return try {
            getGoogleUserFromCredential(
                filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                isAutoSelectEnabled = isAutoSelectEnabled,
            )
        } catch (e: NoCredentialException) {
            logDebug("GoogleAuthUiProvider: NoCredentialException while getting credential")
            if (!filterByAuthorizedAccounts) return null
            try {
                getGoogleUserFromCredential(
                    filterByAuthorizedAccounts = false,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                )
            } catch (_: GetCredentialException) {
                logDebug("GoogleAuthUiProvider: GetCredentialException while getting credential")
                null
            } catch (@Suppress("TooGenericExceptionCaught") _: NullPointerException) {
                logDebug("GoogleAuthUiProvider: NullPointerException while getting credential")
                null
            }
        } catch (_: GetCredentialException) {
            logDebug("GoogleAuthUiProvider: GetCredentialException while getting credential")
            null
        } catch (@Suppress("TooGenericExceptionCaught") _: NullPointerException) {
            logDebug("GoogleAuthUiProvider: NullPointerException while getting credential")
            null
        }
    }

    override suspend fun authorizeSheets(forceConsent: Boolean): GoogleSheetsAuthorizationCode? {
        return try {
            val authorizationResult = fetchAuthorizationResult(forceConsent = forceConsent)
            authorizationResult.serverAuthCode?.let(::GoogleSheetsAuthorizationCode)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            logDebug("GoogleAuthUiProvider: Google Sheets authorization failed")
            null
        }
    }

    private suspend fun getGoogleUserFromCredential(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
    ): GoogleSignInCredential? {
        val credential = credentialManager.getCredential(
            context = activityContext,
            request = getCredentialRequest(
                filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                isAutoSelectEnabled = isAutoSelectEnabled,
            )
        ).credential

        return when {
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val parsedCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleSignInCredential(idToken = parsedCredential.idToken)
                } catch (_: GoogleIdTokenParsingException) {
                    logDebug("GoogleAuthUiProvider: invalid Google ID token response")
                    null
                }
            }

            else -> {
                logDebug("GoogleAuthUiProvider: unsupported credential response")
                null
            }
        }
    }

    private suspend fun fetchAuthorizationResult(forceConsent: Boolean): AuthorizationResult {
        val authClient = Identity.getAuthorizationClient(activityContext)
        val requestBuilder = AuthorizationRequest.builder()
            .setRequestedScopes(GoogleAuthUiProvider.GOOGLE_SHEETS_SCOPES.map(::Scope))
            .requestOfflineAccess(credentials.webClientId)

        if (forceConsent) {
            requestBuilder.setPrompt(AuthorizationRequest.Prompt.CONSENT)
        }
        val initialResult = suspendCancellableCoroutine { continuation ->
            authClient.authorize(requestBuilder.build())
                .addOnSuccessListener(continuation::resume)
                .addOnFailureListener(continuation::resumeWithException)
        }

        if (!initialResult.hasResolution()) return initialResult

        val pendingIntent = initialResult.pendingIntent
            ?: error("Authorization has resolution but no pending intent")
        val activityResult = pendingAuthorizationResult.launchAndAwait {
            scopeIntentLauncher(IntentSenderRequest.Builder(pendingIntent).build())
        }
        return processAuthResult(
            context = activityContext,
            res = activityResult,
        )
    }

    private fun processAuthResult(context: Context, res: ActivityResult): AuthorizationResult {
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            return Identity
                .getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(res.data)
        } else {
            throw IllegalStateException("User cancelled authorization")
        }
    }

    private fun getCredentialRequest(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
    ): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(
                getGoogleIdOption(
                    serverClientId = credentials.webClientId,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                ),
            )
            .build()
    }

    private fun getGoogleIdOption(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
    ): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(isAutoSelectEnabled)
            .setServerClientId(serverClientId)
            .setNonce(createHashedNonceString())
            .build()
    }
}
