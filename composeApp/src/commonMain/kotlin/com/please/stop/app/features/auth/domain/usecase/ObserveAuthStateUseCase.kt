package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeIsAuthenticated()
}
