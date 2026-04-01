package com.please.stop.app.core

import com.please.stop.app.core.models.domain.User
import kotlinx.coroutines.flow.Flow

interface IUserStorage {

    fun read(): Flow<User>

    suspend fun write(user: User)
}
