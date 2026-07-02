package com.please.stop.app.presentation

import androidx.navigation3.runtime.NavKey
import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.features.onboarding.domain.usecase.BackfillCurrencyProfileUseCase
import com.please.stop.app.features.onboarding.domain.usecase.ObserveOnboardingCompletedUseCase
import com.please.stop.app.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.OnboardingRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RootStateHolder(
    private val observeOnboardingCompletedUseCase: ObserveOnboardingCompletedUseCase,
    private val backfillCurrencyProfileUseCase: BackfillCurrencyProfileUseCase,
    private val ioDispatcher: CoroutineDispatcher,
) : StateHolder<RootState, Nothing>() {

    override val tag = "RootStateHolder"
    override val bootstrapTiming = BootstrapTiming.IMMEDIATE

    override fun getInitial() = RootState.Loading

    override suspend fun bootstrap(emit: suspend (Result) -> Unit) {
        backfillCurrencyProfileUseCase()
    }

    override fun collectWhileSubscribed(): Flow<Result> =
        observeOnboardingCompletedUseCase()
            .map { completed ->
                if (completed) RootResult.OnboardingCompleted else RootResult.OnboardingRequired
            }
            .flowOn(ioDispatcher)

    override fun getStateByResult(previous: RootState, result: Result): RootState = when (result) {
        RootResult.OnboardingRequired -> RootState.Ready(initialRoute = OnboardingRoute)
        RootResult.OnboardingCompleted -> RootState.Ready(initialRoute = MainBottomTabs.Home)
        else -> previous
    }

    override fun getErrorStateByResult(result: Result, errorType: ErrorType): RootState =
        RootState.Ready(initialRoute = OnboardingRoute)
}

sealed interface RootState {
    data object Loading : RootState
    data class Ready(val initialRoute: NavKey) : RootState
}

sealed interface RootResult : Result {
    data object OnboardingRequired : RootResult
    data object OnboardingCompleted : RootResult
}
