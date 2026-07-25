package com.please.stop.app.features.auth.data.repository

import com.please.stop.app.features.auth.domain.repository.GoogleAccountRepository
import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions

internal class GoogleAccountRepositoryImpl(
    private val callableFunctions: FirebaseCallableFunctions,
) : GoogleAccountRepository {

    override suspend fun link(authorizationCode: GoogleSheetsAuthorizationCode): Result<Unit> =
        callableFunctions.call(
            functionName = "linkGoogleAccount",
            data = mapOf("authorizationCode" to authorizationCode.value),
        ).map { }

    override suspend fun isLinked(): Result<Boolean> =
        callableFunctions.call(
            functionName = "hasGoogleAccountLink",
            data = emptyMap(),
        ).map { response -> response["linked"] as? Boolean ?: false }

    override suspend fun unlink(): Result<Unit> =
        callableFunctions.call(
            functionName = "unlinkGoogleAccount",
            data = emptyMap(),
        ).map { }
}
