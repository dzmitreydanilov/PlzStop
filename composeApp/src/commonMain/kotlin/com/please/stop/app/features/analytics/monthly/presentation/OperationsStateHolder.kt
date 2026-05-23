package com.please.stop.app.features.analytics.monthly.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.CurrencyDisplayConfig
import com.please.stop.app.features.expenses.domain.model.DayGroup
import com.please.stop.app.features.expenses.domain.model.MonthlyExpenseEntry
import com.please.stop.app.features.expenses.domain.model.MonthlyExpensesData
import com.please.stop.app.features.expenses.domain.usecase.ObserveMonthlyExpensesResult
import com.please.stop.app.features.expenses.domain.usecase.ObserveMonthlyExpensesUseCase
import com.please.stop.app.features.expenses.edit.domain.usecase.DeleteExpenseUseCase
import com.please.stop.app.utils.date.formatDayLabel
import com.please.stop.app.utils.date.isBeforeCurrentMonth
import com.please.stop.app.utils.date.localDateToday
import com.please.stop.app.utils.date.nextMonth
import com.please.stop.app.utils.date.previousMonth
import com.please.stop.app.utils.formatCurrencyAmount
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.number
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import com.please.stop.app.core.models.domain.Result as DomainResult

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class MonthlyExpensesStateHolder(
    private val observeMonthlyExpensesUseCase: ObserveMonthlyExpensesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
) : StateHolder<MonthlyWindowState, OperationsEvent>() {

    override val tag = "MonthlyExpensesStateHolder"
    override val bootstrapTiming = BootstrapTiming.IMMEDIATE

    private val windowFlow = MutableStateFlow(
        localDateToday().let { today -> buildWindow(today.year, today.month.number) }
    )

    override fun getInitial(): MonthlyWindowState = MonthlyWindowState()

    override fun collectFlowsOnInit(): Flow<DomainResult> =
        windowFlow.flatMapLatest { window ->
            if (window.isEmpty()) return@flatMapLatest emptyFlow()
            window.map { (year, month) -> observeMonthlyExpensesUseCase(year, month) }.merge()
        }

    override fun getNavigationResults(): Set<KClass<out DomainResult>> =
        setOf(MonthlyResult.NavigateToEdit::class)

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is MonthlyResult.NavigateToEdit -> OperationsNavigation.OpenEditExpense(result.expenseId)
        else -> null
    }

    override fun getStateByResult(
        previous: MonthlyWindowState,
        result: DomainResult,
    ): MonthlyWindowState = when (result) {
        is MonthlyResult.WindowShifted -> previous.withoutPagesOutside(result.window)

        is ObserveMonthlyExpensesResult.Success -> {
            val prevPageState = previous.pageStateFor(result.year, result.month)
            val expandedIds = when (prevPageState) {
                is MonthPageState.Content -> prevPageState.expandedReceiptIds
                else -> null
            }
            previous.withPage(result.year, result.month, result.data.toPageContent(expandedIds))
        }

        is ObserveMonthlyExpensesResult.Failure -> {
            val prevPageState = previous.pageStateFor(result.year, result.month)
            previous.withPage(
                result.year, result.month,
                MonthPageState.Error(
                    errorType = result.errorType,
                    dayGroups = prevPageState.dayGroups,
                    totalFormatted = prevPageState.totalFormatted,
                    isEmpty = prevPageState.isEmpty,
                    expandedReceiptIds = prevPageState.expandedReceiptIds,
                )
            )
        }

        is MonthlyResult.PageLoading -> previous.withPage(
            result.year,
            result.month,
            MonthPageState.Loading
        )

        is MonthlyResult.ToggleReceipt -> {
            val current =
                previous.pageStateFor(result.year, result.month) as? MonthPageState.Content
                    ?: return super.getStateByResult(previous, result)
            val ids = current.expandedReceiptIds.toPersistentSet()
            val updated =
                if (result.receiptId in ids) ids.remove(result.receiptId) else ids.add(result.receiptId)
            previous.withPage(result.year, result.month, current.copy(expandedReceiptIds = updated))
        }

        is MonthlyResult.ShowDeleteDialog -> previous.copy(deleteConfirmExpenseId = result.expenseId)
        is MonthlyResult.HideDeleteDialog -> previous.copy(deleteConfirmExpenseId = null)
        is DeleteExpenseUseCase.Result.Success -> previous
        is DeleteExpenseUseCase.Result.Failure -> previous

        else -> super.getStateByResult(previous, result)
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType,
    ): MonthlyWindowState = state.value

    override fun resolveEventResult(event: OperationsEvent): Flow<DomainResult> =
        when (event) {
            is OperationsEvent.MonthSelected -> {
                flow {
                    val newWindow = buildWindow(event.year, event.month)
                    val currentState = state.value
                    windowFlow.value = newWindow
                    val newPages = newWindow.filter { (y, m) ->
                        currentState.pages.none { it.year == y && it.month == m }
                    }
                    emit(MonthlyResult.WindowShifted(newWindow))
                    newPages.forEach { (y, m) -> emit(MonthlyResult.PageLoading(y, m)) }
                }
            }

            is OperationsEvent.ExpenseClicked -> {
                flowOf(MonthlyResult.NavigateToEdit(event.expenseId))
            }

            is OperationsEvent.ExpenseLongClicked -> {
                flowOf(MonthlyResult.ShowDeleteDialog(event.expenseId))
            }

            is OperationsEvent.ConfirmDeleteExpense -> flow {
                emit(MonthlyResult.HideDeleteDialog)
                emit(deleteExpenseUseCase(event.expenseId))
            }

            OperationsEvent.DismissDeleteDialog -> {
                flowOf(MonthlyResult.HideDeleteDialog)
            }

            is OperationsEvent.ReceiptGroupClicked -> flowOf(
                MonthlyResult.ToggleReceipt(event.receiptId, event.year, event.month)
            )
        }

    private fun buildWindow(year: Int, month: Int): Set<Pair<Int, Int>> {
        val (prevYear, prevMonth) = previousMonth(year, month)
        val window = mutableSetOf(prevYear to prevMonth, year to month)
        if (isBeforeCurrentMonth(year, month)) {
            val (nextYear, nextMonth) = nextMonth(year, month)
            window.add(nextYear to nextMonth)
        }
        return window
    }

    private fun CurrencyDisplayConfig.format(minorUnits: Long) =
        formatCurrencyAmount(minorUnits, symbol, decimalPlaces)

    private fun MonthlyExpensesData.toPageContent(
        expandedReceiptIds: ImmutableSet<Long>?,
    ): MonthPageState.Content = MonthPageState.Content(
        dayGroups = dayGroups.map { it.toUiModel(currency) }.toImmutableList(),
        totalFormatted = currency.format(totalMinorUnits),
        isEmpty = dayGroups.isEmpty(),
        expandedReceiptIds = expandedReceiptIds ?: persistentSetOf(),
    )

    private fun DayGroup.toUiModel(currency: CurrencyDisplayConfig): DayGroupUiModel =
        DayGroupUiModel(
            dayLabel = formatDayLabel(dayEpochMillis),
            dayEpochMillis = dayEpochMillis,
            totalFormatted = currency.format(totalMinorUnits),
            entries = entries.map { it.toUiModel(currency) }.toImmutableList(),
        )

    private fun MonthlyExpenseEntry.toUiModel(currency: CurrencyDisplayConfig): ExpenseEntryUiModel =
        when (this) {
            is MonthlyExpenseEntry.Single -> ExpenseEntryUiModel.Single(
                id = expense.id,
                title = expense.title,
                categoryName = expense.categoryName,
                categoryIconKey = expense.categoryIconKey,
                amountFormatted = currency.format(expense.amountMinorUnits),
            )

            is MonthlyExpenseEntry.ReceiptGroup -> ExpenseEntryUiModel.ReceiptGroup(
                receiptId = receiptId,
                merchantName = merchantName,
                itemCount = expenses.size,
                amountFormatted = currency.format(totalMinorUnits),
                expenses = expenses.map { item ->
                    ExpenseEntryUiModel.Single(
                        id = item.id,
                        title = item.title,
                        categoryName = item.categoryName,
                        categoryIconKey = item.categoryIconKey,
                        amountFormatted = currency.format(item.amountMinorUnits),
                    )
                }.toImmutableList(),
            )
        }
}

private fun MonthlyWindowState.withPage(
    year: Int,
    month: Int,
    pageState: MonthPageState
): MonthlyWindowState {
    val filtered = pages.filter { it.year != year || it.month != month }
    return copy(pages = (filtered + MonthPageEntry(year, month, pageState)).toImmutableList())
}

private fun MonthlyWindowState.withoutPagesOutside(window: Set<Pair<Int, Int>>): MonthlyWindowState =
    copy(pages = pages.filter { (it.year to it.month) in window }.toImmutableList())

private sealed interface MonthlyResult : DomainResult {
    data class WindowShifted(val window: Set<Pair<Int, Int>>) : MonthlyResult
    data class PageLoading(val year: Int, val month: Int) : MonthlyResult
    data class NavigateToEdit(val expenseId: Long) : MonthlyResult
    data class ToggleReceipt(val receiptId: Long, val year: Int, val month: Int) : MonthlyResult
    data class ShowDeleteDialog(val expenseId: Long) : MonthlyResult
    data object HideDeleteDialog : MonthlyResult
}
