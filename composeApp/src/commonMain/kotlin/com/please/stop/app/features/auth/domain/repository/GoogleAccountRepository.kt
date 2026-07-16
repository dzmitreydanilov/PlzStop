package com.please.stop.app.features.auth.domain.repository

import com.please.stop.app.features.auth.google.GoogleSheetsAuthorizationCode

interface GoogleAccountRepository {
    suspend fun link(authorizationCode: GoogleSheetsAuthorizationCode): Result<Unit>
    suspend fun isLinked(): Result<Boolean>
    suspend fun unlink(): Result<Unit>
}
