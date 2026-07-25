package com.please.stop.app.features.auth.domain.usecase

import com.please.stop.app.features.auth.domain.model.FirebaseSignInProvider
import com.please.stop.app.features.auth.domain.repository.AuthRepository

class GetCurrentSignInProviderUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): FirebaseSignInProvider? = repository.currentSignInProvider()
}
