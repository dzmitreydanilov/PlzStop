package com.please.stop.app.features.home.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.home.domain.model.HomeData
import com.please.stop.app.features.home.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.home.domain.usecase.ObserveHomeDataUseCase
import com.please.stop.app.utils.formatCurrencyAmount
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass

class HomeStateHolder(
    private val observeHomeDataUseCase: ObserveHomeDataUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
) : StateHolder<HomeState, HomeEvent>() {

    override val tag = "HomeStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): HomeState = HomeState.Loading

    override fun collectFlowsOnInit(): Flow<Result> = observeHomeDataUseCase()

    override fun resolveEventResult(event: HomeEvent): Flow<Result> = when (event) {
        is HomeEvent.CategoryClicked -> flowOf(HomeResult.NavigateToAddExpense(event.categoryId))
        HomeEvent.AddExpenseClicked -> flowOf(HomeResult.NavigateToCreateExpense)
        HomeEvent.AddCategoryClicked -> flowOf(HomeResult.ShowAddCategorySheet)
        is HomeEvent.ConfirmAddCategory -> handleAddCategory(event.name)
        HomeEvent.DismissAddCategorySheet -> flowOf(HomeResult.HideAddCategorySheet)
        HomeEvent.DismissError -> flowOf(HomeResult.ClearError)
        HomeEvent.ProfileClicked -> flowOf(HomeResult.NavigateToSettings)
    }

    override fun getNavigationResults(): Set<KClass<out Result>> = setOf(
        HomeResult.NavigateToAddExpense::class,
        HomeResult.NavigateToCreateExpense::class,
        HomeResult.NavigateToSettings::class,
    )

    override fun getNavigationByResult(result: Result): Navigation? = when (result) {
        is HomeResult.NavigateToAddExpense -> HomeNavigation.NavigateToAddExpense(result.categoryId)
        is HomeResult.NavigateToCreateExpense -> HomeNavigation.NavigateToCreateExpense
        is HomeResult.NavigateToSettings -> HomeNavigation.NavigateToSettings
        else -> null
    }

    override fun getStateByResult(previous: HomeState, result: Result): HomeState {
        return when (result) {
            is ObserveHomeDataUseCase.Result.Success -> result.data.toContent(previous)
            is ObserveHomeDataUseCase.Result.Failure -> previous.toError(result.errorType)
            is HomeResult.ShowAddCategorySheet -> previous.updateShowAddCategorySheet(true)
            is HomeResult.HideAddCategorySheet -> previous.updateShowAddCategorySheet(false)
            is HomeResult.ClearError -> previous.toContent()
            is AddCategoryUseCase.Result.Success -> previous.updateShowAddCategorySheet(false)
            is AddCategoryUseCase.Result.Failure -> {
                previous.updateShowAddCategorySheet(false).toError(result.errorType)
            }
            else -> super.getStateByResult(previous, result)
        }
    }

    override fun getErrorStateByResult(result: Result, errorType: ErrorType): HomeState {
        return state.value.toError(errorType)
    }

    private fun handleAddCategory(name: String): Flow<Result> = flow {
        emit(addCategoryUseCase(name.trim(), "ic_other"))
    }

    private fun HomeState.toError(errorType: ErrorType): HomeState.Error = HomeState.Error(
        errorType = errorType,
        displayName = displayName,
        currency = currency,
        totalSpentFormatted = totalSpentFormatted,
        categories = categories,
        hasAnyExpenses = hasAnyExpenses,
        showAddCategorySheet = showAddCategorySheet,
    )

    private fun HomeState.toContent(): HomeState = when (this) {
        is HomeState.Error -> HomeState.Content(
            displayName = displayName,
            currency = currency,
            totalSpentFormatted = totalSpentFormatted,
            categories = categories,
            hasAnyExpenses = hasAnyExpenses,
            showAddCategorySheet = showAddCategorySheet,
        )
        else -> this
    }

    private fun HomeState.updateShowAddCategorySheet(show: Boolean): HomeState = when (this) {
        is HomeState.Content -> copy(showAddCategorySheet = show)
        is HomeState.Error -> copy(showAddCategorySheet = show)
        HomeState.Loading -> this
    }

    private fun HomeData.toContent(previous: HomeState): HomeState.Content {
        return HomeState.Content(
            displayName = displayName.orEmpty(),
            currency = currency,
            totalSpentFormatted = formatCurrencyAmount(
                totalSpentMinorUnits,
                currency.symbol,
                decimalPlaces,
            ),
            categories = categories.map { item ->
                HomeCategoryUiModel(
                    id = item.id,
                    name = item.name,
                    iconKey = item.iconKey,
                    spentFormatted = formatCurrencyAmount(
                        item.spentMinorUnits,
                        currency.symbol,
                        decimalPlaces,
                    ),
                    hasSpending = item.spentMinorUnits > 0,
                )
            }.toImmutableList(),
            hasAnyExpenses = totalSpentMinorUnits > 0,
            showAddCategorySheet = previous.showAddCategorySheet,
        )
    }
}

private sealed interface HomeResult : Result {
    data class NavigateToAddExpense(val categoryId: Long) : HomeResult
    data object NavigateToCreateExpense : HomeResult
    data object NavigateToSettings : HomeResult
    data object ShowAddCategorySheet : HomeResult
    data object HideAddCategorySheet : HomeResult
    data object ClearError : HomeResult
}
