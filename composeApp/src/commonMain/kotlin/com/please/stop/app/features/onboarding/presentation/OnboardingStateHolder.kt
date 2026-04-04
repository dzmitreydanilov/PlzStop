package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.StateSaver
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.onboarding.domain.model.OnboardingData
import com.please.stop.app.features.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.please.stop.app.features.onboarding.domain.usecase.DetectDeviceCurrencyUseCase
import com.please.stop.app.features.onboarding.domain.usecase.LoadOnboardingDataUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.reflect.KClass

class OnboardingStateHolder(
    private val loadOnboardingDataUseCase: LoadOnboardingDataUseCase,
    private val detectDeviceCurrencyUseCase: DetectDeviceCurrencyUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val stateSaver: StateSaver<OnboardingState>,
) : StateHolder<OnboardingState, OnboardingEvent>() {

    override val tag = "OnboardingStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): OnboardingState =
        stateSaver.getState(OnboardingState.Loading, STATE_KEY)

    override fun saveState(state: OnboardingState) {
        stateSaver.saveState(state, STATE_KEY)
    }

    override fun getNavigationResults(): Set<KClass<out Result>> = setOf(
        CompleteOnboardingUseCase.Result.Success::class,
    )

    override fun getNavigationByResult(result: Result): Navigation? = when (result) {
        is CompleteOnboardingUseCase.Result.Success -> OnboardingNavigation.NavigateToHome
        else -> null
    }

    override suspend fun bootstrap(emit: suspend (Result) -> Unit) {
        emit(loadOnboardingDataUseCase())
    }

    override fun resolveEventResult(event: OnboardingEvent): Flow<Result> = when (event) {
        is OnboardingEvent.DisplayNameChanged -> {
            flowOf(
                OnboardingResult.DisplayNameUpdated(
                    event.name.take(OnboardingState.MAX_DISPLAY_NAME_LENGTH)
                )
            )
        }

        is OnboardingEvent.CurrencySelected ->
            flowOf(OnboardingResult.CurrencyChosen(event.currency))

        is OnboardingEvent.BudgetInputChanged ->
            flowOf(OnboardingResult.BudgetInputUpdated(event.input))

        OnboardingEvent.NextTapped -> handleNext()
        OnboardingEvent.SkipTapped -> handleSkip()
        OnboardingEvent.BackTapped -> handleBack()
        OnboardingEvent.SelectCurrencyTapped -> flowOf(OnboardingResult.CurrencySheetOpened)
        OnboardingEvent.CurrencySheetDismissed -> flowOf(OnboardingResult.CurrencySheetClosed)
        OnboardingEvent.ErrorDismissed -> flowOf(OnboardingResult.ErrorCleared)
        OnboardingEvent.RetryLoadCurrencies -> flow { emit(loadOnboardingDataUseCase()) }
    }

    private fun handleNext(): Flow<Result> = flow {
        val state = currentContent() ?: return@flow
        when {
            state.currentStep == OnboardingStep.BUDGET -> {
                emitSaveAndComplete(state)
            }

            state.currentStep == OnboardingStep.WELCOME && !state.hasAttemptedCurrencyDetection -> {
                emit(OnboardingResult.StepAdvanced)
                emit(OnboardingResult.CurrencyDetectionStarted)
                emit(detectDeviceCurrencyUseCase())
            }

            else -> emit(OnboardingResult.StepAdvanced)
        }
    }

    private fun handleSkip(): Flow<Result> = flow {
        val state = currentContent() ?: return@flow
        if (state.currentStep.isSkippable) {
            if (state.currentStep == OnboardingStep.BUDGET) {
                emitSaveAndComplete(state)
            } else {
                emit(OnboardingResult.StepSkipped)
            }
        }
    }

    private suspend fun FlowCollector<Result>.emitSaveAndComplete(state: OnboardingState.Content) {
        emit(OnboardingResult.Saving)
        emit(completeOnboardingUseCase(state.toOnboardingData()))
    }

    private fun handleBack(): Flow<Result> = flow {
        val state = currentContent() ?: return@flow
        if (state.currentStep.previous() != null) {
            emit(OnboardingResult.StepBacked)
        }
    }

    override fun getStateByResult(previous: OnboardingState, result: Result): OnboardingState {
        return when (result) {
            is LoadOnboardingDataUseCase.Result.Success -> {
                val content = (previous as? OnboardingState.Content) ?: OnboardingState.Content()
                content.recalculate()
            }

            is LoadOnboardingDataUseCase.Result.Failure -> reduceError(previous, result.errorType)
            is CompleteOnboardingUseCase.Result.Failure -> reduceError(previous, result.errorType)

            is DetectDeviceCurrencyUseCase.Result.Detected -> {
                val content = previous as? OnboardingState.Content ?: return previous
                content.copy(
                    selectedCurrency = result.currency,
                    currencySymbol = result.currency.symbol,
                    decimalPlaces = result.currency.decimalPlaces,
                    deviceCurrencyCode = result.currency.code,
                    isDetectingCurrency = false,
                    hasAttemptedCurrencyDetection = true,
                ).recalculate()
            }

            is DetectDeviceCurrencyUseCase.Result.NotFound -> {
                val content = previous as? OnboardingState.Content ?: return previous
                content.copy(
                    isDetectingCurrency = false,
                    hasAttemptedCurrencyDetection = true,
                )
            }

            else -> {
                val content = previous as? OnboardingState.Content
                    ?: return super.getStateByResult(previous, result)
                reduceContent(content, result)
            }
        }
    }

    private fun reduceContent(prev: OnboardingState.Content, result: Result): OnboardingState =
        when (result) {
            is OnboardingResult.DisplayNameUpdated -> prev.copy(displayName = result.name)

            is OnboardingResult.CurrencyChosen ->
                prev.copy(
                    selectedCurrency = result.currency,
                    currencySymbol = result.currency.symbol,
                    decimalPlaces = result.currency.decimalPlaces,
                    showCurrencySheet = false,
                ).recalculate()

            is OnboardingResult.BudgetInputUpdated ->
                prev.copy(monthlyBudgetInput = result.input).recalculate()

            is OnboardingResult.StepAdvanced ->
                prev.copy(currentStep = prev.currentStep.next()).recalculate()

            is OnboardingResult.StepSkipped ->
                prev.copy(currentStep = prev.currentStep.next()).recalculate()

            is OnboardingResult.StepBacked -> {
                val previousStep = prev.currentStep.previous() ?: return prev
                prev.copy(currentStep = previousStep).recalculate()
            }

            is OnboardingResult.CurrencySheetOpened ->
                prev.copy(showCurrencySheet = true)

            is OnboardingResult.CurrencySheetClosed ->
                prev.copy(showCurrencySheet = false)

            is OnboardingResult.CurrencyDetectionStarted ->
                prev.copy(isDetectingCurrency = true)

            is OnboardingResult.Saving -> prev.copy(isSaving = true, error = null)
            is OnboardingResult.ErrorCleared -> prev.copy(error = null)

            else -> super.getStateByResult(prev, result)
        }

    private fun reduceError(previous: OnboardingState, errorType: ErrorType): OnboardingState {
        val content = previous as? OnboardingState.Content
        return if (content != null) {
            val context = if (content.isSaving) {
                OnboardingError.ErrorContext.SAVE_ONBOARDING
            } else {
                OnboardingError.ErrorContext.LOAD_CURRENCIES
            }
            content.copy(
                isSaving = false,
                error = OnboardingError(errorType = errorType, context = context),
            )
        } else {
            OnboardingState.Error(
                error = OnboardingError(
                    errorType = errorType,
                    context = OnboardingError.ErrorContext.LOAD_CURRENCIES,
                )
            )
        }
    }

    override fun getErrorStateByResult(result: Result, errorType: ErrorType): OnboardingState {
        return OnboardingState.Error(
            error = OnboardingError(
                errorType = errorType,
                context = OnboardingError.ErrorContext.LOAD_CURRENCIES,
            )
        )
    }

    private fun OnboardingState.Content.recalculate(): OnboardingState.Content {
        return copy(isNextEnabled = computeIsNextEnabled())
    }

    private fun OnboardingState.Content.computeIsNextEnabled(): Boolean = when (currentStep) {
        OnboardingStep.WELCOME -> true
        OnboardingStep.CURRENCY -> selectedCurrency != null
        OnboardingStep.NAME -> true
        OnboardingStep.BUDGET -> isBudgetValid(monthlyBudgetInput)
    }

    private fun currentContent(): OnboardingState.Content? {
        val saved = stateSaver.getState(OnboardingState.Loading, STATE_KEY)
        return saved as? OnboardingState.Content
    }

    private fun OnboardingState.Content.toOnboardingData(): OnboardingData {
        val currency = checkNotNull(selectedCurrency) { "Currency must be selected" }
        val budgetValue = monthlyBudgetInput.toDoubleOrNull() ?: 0.0
        val minorUnits = (budgetValue * 10.0.pow(currency.decimalPlaces)).roundToLong()

        return OnboardingData(
            displayName = displayName.takeIf { it.isNotBlank() },
            currency = currency,
            monthlyBudgetMinorUnits = minorUnits,
        )
    }

    companion object {
        internal const val STATE_KEY = "onboarding_state"

        private fun isBudgetValid(input: String): Boolean {
            if (input.isBlank()) return false
            val value = input.toDoubleOrNull() ?: return false
            return value > 0 && value <= OnboardingState.MAX_BUDGET
        }
    }
}
