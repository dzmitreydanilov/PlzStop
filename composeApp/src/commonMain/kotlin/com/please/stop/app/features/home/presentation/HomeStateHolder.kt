package com.please.stop.app.features.home.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.home.domain.model.HomeData
import com.please.stop.app.features.home.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.home.domain.usecase.ObserveHomeDataUseCase
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.math.pow
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
        HomeEvent.AddExpenseClicked -> flowOf(HomeResult.NavigateToAddExpense())
        HomeEvent.AddCategoryClicked -> flowOf(HomeResult.ShowAddCategorySheet)
        is HomeEvent.ConfirmAddCategory -> handleAddCategory(event.name)
        HomeEvent.DismissAddCategorySheet -> flowOf(HomeResult.HideAddCategorySheet)
        HomeEvent.DismissError -> flowOf(HomeResult.ClearError)
        HomeEvent.ProfileClicked -> flowOf(HomeResult.NavigateToSettings)
    }

    override fun getNavigationResults(): Set<KClass<out Result>> = setOf(
        HomeResult.NavigateToAddExpense::class,
        HomeResult.NavigateToSettings::class,
    )

    override fun getNavigationByResult(result: Result): Navigation? = when (result) {
        is HomeResult.NavigateToAddExpense -> HomeNavigation.NavigateToAddExpense(result.categoryId)
        is HomeResult.NavigateToSettings -> HomeNavigation.NavigateToSettings
        else -> null
    }

    override fun getStateByResult(previous: HomeState, result: Result): HomeState {
        val content = previous as? HomeState.Content
        return when (result) {
            is ObserveHomeDataUseCase.Result.Success -> result.data.toContent(previous)
            is ObserveHomeDataUseCase.Result.Failure -> HomeState.Error(result.errorType)
            is HomeResult.ShowAddCategorySheet -> {
                content?.copy(showAddCategorySheet = true) ?: previous
            }
            is HomeResult.HideAddCategorySheet -> {
                content?.copy(showAddCategorySheet = false) ?: previous
            }
            is HomeResult.ClearError -> {
                content?.copy(errorType = null) ?: previous
            }
            is AddCategoryUseCase.Result.Success -> {
                content?.copy(showAddCategorySheet = false) ?: previous
            }
            is AddCategoryUseCase.Result.Failure -> {
                content?.copy(
                    showAddCategorySheet = false,
                    errorType = result.errorType,
                ) ?: previous
            }
            else -> super.getStateByResult(previous, result)
        }
    }

    override fun getErrorStateByResult(result: Result, errorType: ErrorType): HomeState {
        return HomeState.Error(errorType)
    }

    private fun handleAddCategory(name: String): Flow<Result> = flow {
        emit(addCategoryUseCase(name.trim(), "ic_other"))
    }

    private fun HomeData.toContent(previous: HomeState): HomeState.Content {
        val prev = previous as? HomeState.Content
        return HomeState.Content(
            displayName = displayName,
            currencySymbol = currencySymbol,
            decimalPlaces = decimalPlaces,
            totalSpentFormatted = formatAmount(totalSpentMinorUnits, currencySymbol, decimalPlaces),
            categories = categories.map { item ->
                HomeCategoryUiModel(
                    id = item.id,
                    name = item.name,
                    iconKey = item.iconKey,
                    spentFormatted = formatAmount(
                        item.spentMinorUnits,
                        currencySymbol,
                        decimalPlaces
                    ),
                    hasSpending = item.spentMinorUnits > 0,
                )
            }.toImmutableList(),
            hasAnyExpenses = totalSpentMinorUnits > 0,
            showAddCategorySheet = prev?.showAddCategorySheet ?: false,
            errorType = prev?.errorType,
        )
    }

    companion object {
        internal fun formatAmount(
            minorUnits: Long,
            currencySymbol: String,
            decimalPlaces: Int,
        ): String {
            if (decimalPlaces == 0) return "$currencySymbol$minorUnits"
            val divisor = 10.0.pow(decimalPlaces)
            val value = minorUnits / divisor
            val formatted = value.toBigDecimalString(decimalPlaces)
            return "$currencySymbol$formatted"
        }

        private fun Double.toBigDecimalString(decimalPlaces: Int): String {
            val intPart = toLong()
            val fracPart = ((this - intPart) * 10.0.pow(decimalPlaces))
                .toLong()
                .toString()
                .padStart(decimalPlaces, '0')
            return "$intPart.$fracPart"
        }
    }
}

private sealed interface HomeResult : Result {
    data class NavigateToAddExpense(val categoryId: Long? = null) : HomeResult
    data object NavigateToSettings : HomeResult
    data object ShowAddCategorySheet : HomeResult
    data object HideAddCategorySheet : HomeResult
    data object ClearError : HomeResult
}
