package com.please.stop.app.features.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.please.stop.app.features.auth.google.GoogleAuthCredentials
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.features.auth.google.GoogleAuthUiProvider
import com.please.stop.app.features.auth.google.PendingAuthorizationResult

internal class GoogleAuthProviderImpl(
    private val credentials: GoogleAuthCredentials,
    private val credentialManager: CredentialManager,
) : GoogleAuthProvider {

    @Composable
    override fun getUiProvider(): GoogleAuthUiProvider {
        val activityContext = LocalContext.current
        val pendingAuthorizationResult = remember {
            PendingAuthorizationResult<ActivityResult>()
        }
        val scopeIntentLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result -> pendingAuthorizationResult.complete(result) }

        return GoogleAuthUiProviderImpl(
            activityContext = activityContext,
            credentialManager = credentialManager,
            credentials = credentials,
            scopeIntentLauncher = scopeIntentLauncher::launch,
            pendingAuthorizationResult = pendingAuthorizationResult,
        )
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
