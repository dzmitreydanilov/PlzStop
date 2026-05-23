package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.core.IGoogleAccountStorage
import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.features.auth.data.DeleteAccountResult
import com.please.stop.app.features.auth.data.FirebaseAuthProvider
import com.please.stop.app.features.auth.google.GoogleAuthProvider
import com.please.stop.app.network.authentication.BearerTokenClearer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DeleteAccountUseCase(
    private val firebaseAuthProvider: FirebaseAuthProvider,
    private val googleAccountStorage: IGoogleAccountStorage,
    private val bearerTokenClearer: BearerTokenClearer,
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
        when (val deleteResult = firebaseAuthProvider.deleteAccount()) {
            is DeleteAccountResult.Success -> {
                googleAccountStorage.delete()
                bearerTokenClearer.clear()
                googleAuthProvider.signOut()
                appDatabase.clearAllData()
                DeleteResult.Success
            }
            is DeleteAccountResult.NeedsReauthentication -> DeleteResult.NeedsReauthentication
            is DeleteAccountResult.Failure -> DeleteResult.Failure(deleteResult.error.toErrorType())
        }
    }
}
