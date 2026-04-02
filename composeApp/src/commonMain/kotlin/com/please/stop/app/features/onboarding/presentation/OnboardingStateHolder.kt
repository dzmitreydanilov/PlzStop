package com.please.stop.app.features.onboarding.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.StateSaver
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.onboarding.domain.model.Category
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.model.OnboardingData
import com.please.stop.app.features.onboarding.domain.usecase.CompleteOnboardingUseCase
import com.please.stop.app.features.onboarding.domain.usecase.LoadOnboardingDataUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.pow
import kotlin.reflect.KClass

class OnboardingStateHolder(
    private val loadOnboardingDataUseCase: LoadOnboardingDataUseCase,
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
        is OnboardingEvent.DisplayNameChanged ->
            flowOf(OnboardingResult.DisplayNameUpdated(event.name.take(OnboardingState.MAX_DISPLAY_NAME_LENGTH)))

        is OnboardingEvent.CurrencySearchQueryChanged ->
            flowOf(OnboardingResult.CurrencySearchUpdated(event.query))

        is OnboardingEvent.CurrencySelected ->
            flowOf(OnboardingResult.CurrencyChosen(event.currency))

        is OnboardingEvent.BudgetInputChanged ->
            flowOf(OnboardingResult.BudgetInputUpdated(event.input))

        is OnboardingEvent.CategoryToggled ->
            flowOf(OnboardingResult.CategoryToggled(event.categoryId))

        is OnboardingEvent.CustomCategoryAdded ->
            flowOf(
                OnboardingResult.CustomCategoryAdded(
                    CategoryUiModel(
                        id = generateTempId(),
                        name = event.name,
                        iconKey = event.iconKey,
                        isDefault = false,
                    )
                )
            )

        OnboardingEvent.NextTapped -> handleNext()
        OnboardingEvent.BackTapped -> handleBack()
        OnboardingEvent.ErrorDismissed -> flowOf(OnboardingResult.ErrorCleared)
        OnboardingEvent.RetryLoadCurrencies -> flow { emit(loadOnboardingDataUseCase()) }
    }

    private fun handleNext(): Flow<Result> = flow {
        val state = currentContent() ?: return@flow
        if (state.currentStep == OnboardingStep.CATEGORIES) {
            emit(OnboardingResult.Saving)
            emit(completeOnboardingUseCase(state.toOnboardingData()))
        } else {
            emit(OnboardingResult.StepAdvanced)
        }
    }

    private fun handleBack(): Flow<Result> = flow {
        val state = currentContent() ?: return@flow
        if (state.currentStep.previous() != null) {
            emit(OnboardingResult.StepBacked)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun getStateByResult(previous: OnboardingState, result: Result): OnboardingState {
        return when (result) {
            is LoadOnboardingDataUseCase.Result.Success -> {
                val content = (previous as? OnboardingState.Content) ?: OnboardingState.Content()
                content.copy(
                    currencies = result.currencies,
                    availableCategories = result.categories,
                ).recalculate()
            }

            is LoadOnboardingDataUseCase.Result.Failure -> reduceError(previous, result.errorType)
            is CompleteOnboardingUseCase.Result.Failure -> reduceError(previous, result.errorType)

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

            is OnboardingResult.CurrencySearchUpdated ->
                prev.copy(currencySearchQuery = result.query).recalculate()

            is OnboardingResult.CurrencyChosen ->
                prev.copy(
                    selectedCurrency = result.currency,
                    currencySymbol = result.currency.symbol,
                    decimalPlaces = result.currency.decimalPlaces,
                ).recalculate()

            is OnboardingResult.BudgetInputUpdated ->
                prev.copy(monthlyBudgetInput = result.input).recalculate()

            is OnboardingResult.CategoryToggled -> {
                val mutable = prev.selectedCategoryIds.toMutableSet()
                if (result.categoryId in mutable) mutable.remove(result.categoryId) else mutable.add(result.categoryId)
                prev.copy(selectedCategoryIds = mutable.toPersistentSet()).recalculate()
            }

            is OnboardingResult.CustomCategoryAdded ->
                prev.copy(
                    customCategories = (prev.customCategories + result.category).toImmutableList(),
                ).recalculate()

            is OnboardingResult.StepAdvanced ->
                prev.copy(
                    currentStep = prev.currentStep.next(),
                    currencySearchQuery = "",
                ).recalculate()

            is OnboardingResult.StepBacked -> {
                val previousStep = prev.currentStep.previous() ?: return prev
                prev.copy(currentStep = previousStep).recalculate()
            }

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
        val filtered = filterCurrencies(currencies, currencySearchQuery)
        return copy(
            popularCurrencies = filtered.filter { it.isPopular }.toImmutableList(),
            otherCurrencies = filtered.filter { !it.isPopular }.toImmutableList(),
            isNextEnabled = computeIsNextEnabled(),
            canAddCustomCategory = customCategories.size < OnboardingState.MAX_CUSTOM_CATEGORIES,
        )
    }

    private fun OnboardingState.Content.computeIsNextEnabled(): Boolean = when (currentStep) {
        OnboardingStep.WELCOME -> true
        OnboardingStep.CURRENCY -> selectedCurrency != null
        OnboardingStep.BUDGET -> isBudgetValid(monthlyBudgetInput)
        OnboardingStep.CATEGORIES -> selectedCategoryIds.size >= OnboardingState.MIN_CATEGORIES
    }

    private fun currentContent(): OnboardingState.Content? {
        val saved = stateSaver.getState(OnboardingState.Loading, STATE_KEY)
        return saved as? OnboardingState.Content
    }

    private fun OnboardingState.Content.toOnboardingData(): OnboardingData {
        val currency = checkNotNull(selectedCurrency) { "Currency must be selected" }
        val budgetValue = monthlyBudgetInput.toDouble()
        val minorUnits = (budgetValue * 10.0.pow(currency.decimalPlaces)).toLong()

        val allCategories = availableCategories + customCategories
        val selected = allCategories.filter { it.id in selectedCategoryIds }

        return OnboardingData(
            displayName = displayName.takeIf { it.isNotBlank() },
            currency = currency,
            monthlyBudgetMinorUnits = minorUnits,
            selectedCategories = selected.map {
                Category(
                    id = it.id,
                    name = it.name,
                    iconKey = it.iconKey,
                    isDefault = it.isDefault,
                )
            },
        )
    }

    companion object {
        internal const val STATE_KEY = "onboarding_state"
        private var tempIdCounter = -1L
        private fun generateTempId(): Long = tempIdCounter--

        private fun filterCurrencies(
            currencies: ImmutableList<Currency>,
            query: String,
        ): ImmutableList<Currency> {
            if (query.isBlank()) return currencies
            val q = query.trim().lowercase()
            return currencies.filter {
                it.code.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.symbol.lowercase().contains(q)
            }.toImmutableList()
        }

        private fun isBudgetValid(input: String): Boolean {
            if (input.isBlank()) return false
            val value = input.toDoubleOrNull() ?: return false
            return value > 0 && value <= OnboardingState.MAX_BUDGET
        }
    }
}
