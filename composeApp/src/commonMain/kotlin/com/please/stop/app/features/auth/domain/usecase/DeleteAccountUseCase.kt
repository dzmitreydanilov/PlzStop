package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.core.logger.logWarningWithTag
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.model.FirebaseReauthenticationCredential
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DeleteAccountUseCase(
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAccountStorage: IGoogleAccountStorage,
    private val googleAuthProvider: GoogleAuthProvider,
    private val appDatabase: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface DeleteResult : Result {
        data object Success : DeleteResult
        data object NeedsReauthentication : DeleteResult
        data class Failure(override val errorType: ErrorType) : DeleteResult, ErrorResult
    }

    suspend operator fun invoke(): DeleteResult = withContext(ioDispatcher) {
        deleteFirebaseUser()
    }

    suspend operator fun invoke(credential: FirebaseReauthenticationCredential): DeleteResult =
        withContext(ioDispatcher) {
            firebaseAuthProvider.reauthenticate(credential).fold(
                onSuccess = { deleteFirebaseUser() },
                onFailure = { error -> DeleteResult.Failure(error.toErrorType()) },
            )
        }

    private suspend fun deleteFirebaseUser(): DeleteResult =
        // The Auth user-deletion trigger revokes and removes any server-side Google grant.
        when (val deleteResult = firebaseAuthProvider.deleteAccount()) {
            is DeleteAccountResult.Success -> {
                val databaseCleanupResult = runSuspendCatching { appDatabase.clearUserData() }
                runSuspendCatching { googleAccountStorage.delete() }.logBestEffortFailure(
                    operation = "local Google link cleanup",
                )
                runSuspendCatching { googleAuthProvider.signOut() }.logBestEffortFailure(
                    operation = "Google credential-state cleanup",
                )
                databaseCleanupResult.fold(
                    onSuccess = { DeleteResult.Success },
                    onFailure = { error -> DeleteResult.Failure(error.toErrorType()) },
                )
            }

            is DeleteAccountResult.NeedsReauthentication -> DeleteResult.NeedsReauthentication
            is DeleteAccountResult.Failure -> DeleteResult.Failure(deleteResult.error.toErrorType())
        }
}

private fun kotlin.Result<Unit>.logBestEffortFailure(operation: String) {
    onFailure {
        logWarningWithTag(
            tag = "DeleteAccountUseCase",
            message = "$operation failed after Firebase account deletion",
        )
    }
}

internal suspend fun FirebaseAuthProvider.reauthenticate(
    credential: FirebaseReauthenticationCredential,
): kotlin.Result<Unit> = when (credential) {
    is FirebaseReauthenticationCredential.Google -> reauthenticateWithGoogle(credential.idToken)
    is FirebaseReauthenticationCredential.Apple -> reauthenticateWithApple(
        identityToken = credential.identityToken,
        nonce = credential.nonce,
    )
}
