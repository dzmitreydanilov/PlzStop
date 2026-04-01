package com.please.stop.app.core

import com.please.stop.app.core.models.data.AuthToken
import kotlinx.coroutines.flow.Flow

interface ITokensStorage {

    fun readAuthToken(): Flow<AuthToken>
    fun readAuthTokenBlocking(): AuthToken?
    suspend fun writeAuthToken(token: AuthToken)
    suspend fun deleteAuthToken()
}
