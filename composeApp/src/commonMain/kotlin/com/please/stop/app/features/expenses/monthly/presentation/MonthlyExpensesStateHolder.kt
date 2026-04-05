package com.please.stop.app.features.expenses.monthly.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.CurrencyDisplayConfig
import com.please.stop.app.features.expenses.domain.model.DayGroup
import com.please.stop.app.features.expenses.domain.model.MonthlyExpenseEntry
import com.please.stop.app.features.expenses.domain.model.MonthlyExpensesData
import com.please.stop.app.features.expenses.domain.usecase.ObserveMonthlyExpensesUseCase
import com.please.stop.app.utils.date.formatDayLabel
import com.please.stop.app.utils.formatCurrencyAmount
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import com.please.stop.app.core.models.domain.Result as DomainResult

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class MonthlyExpensesStateHolder(
    private val observeMonthlyExpensesUseCase: ObserveMonthlyExpensesUseCase,
) : StateHolder<MonthlyExpensesState, MonthlyExpensesEvent>() {

    override val tag = "MonthlyExpensesStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val selectedMonthFlow = MutableStateFlow(today.year to today.monthNumber)

    override fun getInitial(): MonthlyExpensesState = MonthlyExpensesState.Loading

    override fun collectFlowsOnInit(): Flow<DomainResult> {
        return selectedMonthFlow.flatMapLatest { (year, month) ->
            observeMonthlyExpensesUseCase(year, month)
        }
    }

    override fun getNavigationResults(): Set<KClass<out DomainResult>> =
        setOf(MonthlyResult.NavigateToEdit::class)

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is MonthlyResult.NavigateToEdit -> MonthlyExpensesNavigation.OpenEditExpense(result.expenseId)
        else -> null
    }

    override fun getStateByResult(
        previous: MonthlyExpensesState,
        result: DomainResult,
    ): MonthlyExpensesState = when (result) {
        is ObserveMonthlyExpensesUseCase.Result.Success -> {
            val expandedIds = (previous as? MonthlyExpensesState.Content)?.expandedReceiptIds
            result.data.toContent(expandedIds)
        }
        is ObserveMonthlyExpensesUseCase.Result.Failure -> MonthlyExpensesState.Error(result.errorType)
        is MonthlyResult.Loading -> MonthlyExpensesState.Loading
        is MonthlyResult.ToggleReceipt -> {
            val content = previous as? MonthlyExpensesState.Content ?: return super.getStateByResult(previous, result)
            val current = content.expandedReceiptIds.toPersistentSet()
            val updated = if (result.receiptId in current) current.remove(result.receiptId) else current.add(result.receiptId)
            content.copy(expandedReceiptIds = updated)
        }
        else -> super.getStateByResult(previous, result)
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType,
    ): MonthlyExpensesState = MonthlyExpensesState.Error(errorType)

    override fun resolveEventResult(event: MonthlyExpensesEvent): Flow<DomainResult> = when (event) {
        is MonthlyExpensesEvent.PreviousMonthClicked -> flow {
            val (year, month) = selectedMonthFlow.value
            val (newYear, newMonth) = if (month == 1) year - 1 to 12 else year to month - 1
            emit(MonthlyResult.Loading)
            selectedMonthFlow.value = newYear to newMonth
        }
        is MonthlyExpensesEvent.NextMonthClicked -> flow {
            val (year, month) = selectedMonthFlow.value
            val now = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val isCurrentOrFuture = year > now.year || (year == now.year && month >= now.monthNumber)
            if (isCurrentOrFuture) return@flow
            val (newYear, newMonth) = if (month == 12) year + 1 to 1 else year to month + 1
            emit(MonthlyResult.Loading)
            selectedMonthFlow.value = newYear to newMonth
        }
        is MonthlyExpensesEvent.ExpenseClicked -> flowOf(MonthlyResult.NavigateToEdit(event.expenseId))
        is MonthlyExpensesEvent.ReceiptGroupClicked -> flowOf(MonthlyResult.ToggleReceipt(event.receiptId))
    }

    private fun MonthlyExpensesData.toContent(
        expandedReceiptIds: kotlinx.collections.immutable.ImmutableSet<Long>?,
    ): MonthlyExpensesState.Content {
        val now = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val canGoToNextMonth = year < now.year || (year == now.year && month < now.monthNumber)
        return MonthlyExpensesState.Content(
            year = year,
            month = month,
            monthLabel = "${monthName(month)} $year",
            canGoToPreviousMonth = true,
            canGoToNextMonth = canGoToNextMonth,
            dayGroups = dayGroups.map { it.toUiModel(currency) }.toImmutableList(),
            totalFormatted = formatCurrencyAmount(totalMinorUnits, currency.symbol, currency.decimalPlaces),
            isEmpty = dayGroups.isEmpty(),
            expandedReceiptIds = expandedReceiptIds ?: kotlinx.collections.immutable.persistentSetOf(),
        )
    }

    private fun DayGroup.toUiModel(currency: CurrencyDisplayConfig): DayGroupUiModel {
        return DayGroupUiModel(
            dayLabel = formatDayLabel(dayEpochMillis),
            dayEpochMillis = dayEpochMillis,
            totalFormatted = formatCurrencyAmount(totalMinorUnits, currency.symbol, currency.decimalPlaces),
            entries = entries.map { it.toUiModel(currency) }.toImmutableList(),
        )
    }

    private fun MonthlyExpenseEntry.toUiModel(currency: CurrencyDisplayConfig): ExpenseEntryUiModel {
        return when (this) {
            is MonthlyExpenseEntry.Single -> ExpenseEntryUiModel.Single(
                id = expense.id,
                title = expense.title,
                categoryName = expense.categoryName,
                categoryIconKey = expense.categoryIconKey,
                amountFormatted = formatCurrencyAmount(expense.amountMinorUnits, currency.symbol, currency.decimalPlaces),
            )
            is MonthlyExpenseEntry.ReceiptGroup -> ExpenseEntryUiModel.ReceiptGroup(
                receiptId = receiptId,
                merchantName = merchantName ?: "Receipt",
                itemCount = expenses.size,
                amountFormatted = formatCurrencyAmount(totalMinorUnits, currency.symbol, currency.decimalPlaces),
                expenses = expenses.map { item ->
                    ExpenseEntryUiModel.Single(
                        id = item.id,
                        title = item.title,
                        categoryName = item.categoryName,
                        categoryIconKey = item.categoryIconKey,
                        amountFormatted = formatCurrencyAmount(item.amountMinorUnits, currency.symbol, currency.decimalPlaces),
                    )
                }.toImmutableList(),
            )
        }
    }
}

private fun monthName(month: Int): String = when (month) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    else -> "December"
}

private sealed interface MonthlyResult : DomainResult {
    data object Loading : MonthlyResult
    data class NavigateToEdit(val expenseId: Long) : MonthlyResult
    data class ToggleReceipt(val receiptId: Long) : MonthlyResult
}
