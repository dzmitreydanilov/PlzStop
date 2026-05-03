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
import com.please.stop.app.features.auth.google.GoogleUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class GoogleAuthUiProviderImpl(
    private val activityContext: Context,
    private val credentialManager: CredentialManager,
    private val credentials: GoogleAuthCredentials,
    private val scopeIntentLauncher: (IntentSenderRequest) -> Unit,
    private val authResultChannel: ReceiveChannel<ActivityResult>
) : GoogleAuthUiProvider {

    override suspend fun signIn(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {

        val googleUser = try {
            getGoogleUserFromCredential(
                filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                isAutoSelectEnabled = isAutoSelectEnabled,
                scopes = scopes
            )
        } catch (e: NoCredentialException) {
            logDebug("GoogleAuthUiProvider: NoCredentialException while getting credential")
            if (!filterByAuthorizedAccounts)
                return handleCredentialException(e = e)
            try {
                getGoogleUserFromCredential(
                    filterByAuthorizedAccounts = false,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                    scopes = scopes
                )
            } catch (e: GetCredentialException) {
                logDebug("GoogleAuthUiProvider: GetCredentialException while getting credential")
                handleCredentialException(e = e)
            } catch (@Suppress("TooGenericExceptionCaught") e: NullPointerException) {
                logDebug("GoogleAuthUiProvider: NullPointerException while getting credential")
                null
            }
        } catch (e: GetCredentialException) {
            logDebug("GoogleAuthUiProvider: GetCredentialException while getting credential")
            handleCredentialException(e = e)
        } catch (@Suppress("TooGenericExceptionCaught") e: NullPointerException) {
            logDebug("GoogleAuthUiProvider: NullPointerException while getting credential")
            null
        }
        return googleUser
    }

    private fun handleCredentialException(
        e: GetCredentialException,
    ): GoogleUser? {
        logDebug("GoogleAuthUiProvider error: $e and message: ${e.message}")
        return null
    }

    private suspend fun getGoogleUserFromCredential(
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean,
        scopes: List<String>
    ): GoogleUser? {
        val credential = credentialManager.getCredential(
            context = activityContext,
            request = getCredentialRequest(
                filterByAuthorizedAccounts,
                isAutoSelectEnabled,
            )

        ).credential

        logDebug("GoogleAuthUiProvider Received Credential: $credential")

        return when {
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val accessToken =
                        if (scopes != GoogleAuthUiProvider.BASIC_AUTH_SCOPE) {
                            fetchAccessTokenWithScopes(
                                scopes
                            ).accessToken
                        } else {
                            null
                        }

                    GoogleUser(
                        idToken = googleIdTokenCredential.idToken,
                        accessToken = accessToken
                    )
                } catch (e: GoogleIdTokenParsingException) {
                    logDebug("GoogleAuthUiProvider Received an invalid google id token response: ${e.message}")
                    null
                }
            }

            else -> {
                logDebug("GoogleAuthUiProvider Received an invalid credential response: ${credential.type}")
                null
            }
        }
    }

    private suspend fun fetchAccessTokenWithScopes(scopes: List<String>): AuthorizationResult {
        val authClient = Identity.getAuthorizationClient(activityContext)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map(::Scope))
            .build()

        return suspendCancellableCoroutine { continuation ->
            authClient.authorize(request)
                .addOnSuccessListener { r ->
                    if (r.hasResolution()) {
                        r.pendingIntent?.let { intent ->
                            scopeIntentLauncher(IntentSenderRequest.Builder(intent).build())
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val result = authResultChannel.receive()
                                    val authResult = processAuthResult(activityContext, result)
                                    continuation.resume(authResult)
                                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                                    if (e is CancellationException) throw e
                                    continuation.resumeWithException(e)
                                }
                            }
                        } ?: run {
                            continuation.resumeWithException(
                                IllegalStateException("Authorization has resolution but no pending intent")
                            )
                        }
                    } else {
                        continuation.resume(r)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }

            continuation.invokeOnCancellation {
                authResultChannel.cancel()
            }
        }
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
        isAutoSelectEnabled: Boolean
    ): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(
                getGoogleIdOption(
                    serverClientId = credentials.webClientId,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    isAutoSelectEnabled = isAutoSelectEnabled,
                )
            )
            .build()
    }

    private fun getGoogleIdOption(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
        isAutoSelectEnabled: Boolean
    ): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(isAutoSelectEnabled)
            .setServerClientId(serverClientId)
            .setNonce(createHashedNonceString())
            .build()
    }
}
