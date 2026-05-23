package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.flow.flowFromSuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class CheckGoogleAccountLinkageUseCase(
    private val dispatcher: CoroutineDispatcher
) {

    operator fun invoke(): Flow<HasGoogleAccountLinkageResult> {
        return flowFromSuspend {
            false
        }.map { hasGoogleAccountLinkage ->
            HasGoogleAccountLinkageResult.GoogleAccountLinked.takeIf { hasGoogleAccountLinkage }
                ?: HasGoogleAccountLinkageResult.GoogleAccountNotLinked
        }.flowOn(dispatcher)
    }
}

sealed interface HasGoogleAccountLinkageResult : com.please.stop.app.core.models.domain.Result {
    data object GoogleAccountLinked : HasGoogleAccountLinkageResult
    data object GoogleAccountNotLinked : HasGoogleAccountLinkageResult
}
