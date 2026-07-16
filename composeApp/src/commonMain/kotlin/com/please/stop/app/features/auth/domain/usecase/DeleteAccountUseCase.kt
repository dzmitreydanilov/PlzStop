package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.core.runSuspendCatching
import com.please.stop.app.features.auth.apple.AppleUser
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.features.auth.google.GoogleSignInCredential
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DeleteAccountUseCase(
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAccountStorage: IGoogleAccountStorage,
    private val googleAccountRepository: GoogleAccountRepository,
    private val googleAuthProvider: GoogleAuthProvider,
    private val appDatabase: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) {
    sealed interface DeleteResult : Result {
        data object Success : DeleteResult
        data object NeedsReauthentication : DeleteResult
        data class Failure(override val errorType: ErrorType) : DeleteResult, ErrorResult
    }

    suspend operator fun invoke(credential: GoogleSignInCredential): DeleteResult =
        withContext(ioDispatcher) {
            firebaseAuthProvider.reauthenticateWithGoogle(credential.idToken).fold(
                onSuccess = { deleteAfterReauthentication() },
                onFailure = { error -> DeleteResult.Failure(error.toErrorType()) },
            )
        }

    suspend operator fun invoke(credential: AppleUser): DeleteResult =
        withContext(ioDispatcher) {
            firebaseAuthProvider.reauthenticateWithApple(
                identityToken = credential.identityToken,
                nonce = credential.nonce,
            ).fold(
                onSuccess = { deleteAfterReauthentication() },
                onFailure = { error -> DeleteResult.Failure(error.toErrorType()) },
            )
        }

    private suspend fun deleteAfterReauthentication(): DeleteResult {
        // Best effort: the backend Auth deletion trigger removes any record if this call is offline.
        googleAccountRepository.unlink()

        return when (val deleteResult = firebaseAuthProvider.deleteAccount()) {
            is DeleteAccountResult.Success -> {
                val cleanupResults = listOf(
                    runSuspendCatching { googleAccountStorage.delete() },
                    runSuspendCatching { googleAuthProvider.signOut() },
                    runSuspendCatching { appDatabase.clearAllData() },
                )
                cleanupResults.firstNotNullOfOrNull { result ->
                    result.exceptionOrNull()
                }?.let { error ->
                    DeleteResult.Failure(error.toErrorType())
                } ?: DeleteResult.Success
            }

            is DeleteAccountResult.NeedsReauthentication -> DeleteResult.NeedsReauthentication
            is DeleteAccountResult.Failure -> DeleteResult.Failure(deleteResult.error.toErrorType())
        }
    }
}
