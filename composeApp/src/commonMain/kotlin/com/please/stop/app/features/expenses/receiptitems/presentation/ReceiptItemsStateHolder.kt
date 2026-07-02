package com.please.stop.app.features.expenses.receiptitems.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import com.please.stop.app.features.expenses.domain.model.ReceiptExpenseItem
import com.please.stop.app.features.expenses.domain.usecase.ConsumePendingReceiptDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataResult
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveReceiptExpensesUseCase
import com.please.stop.app.features.expenses.presentation.AddExpenseStateMapper
import com.please.stop.app.features.expenses.presentation.CurrencyConfig
import com.please.stop.app.features.expenses.presentation.KeyboardCalculator
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.date.localDateToday
import com.please.stop.app.utils.date.monthMillisRange
import com.please.stop.app.utils.date.nowMillis
import com.please.stop.app.utils.minorUnitsMultiplier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlin.reflect.KClass
import com.please.stop.app.core.models.domain.Result as DomainResult

class ReceiptItemsStateHolder(
    private val consumePendingReceiptDataUseCase: ConsumePendingReceiptDataUseCase,
    private val saveReceiptExpensesUseCase: SaveReceiptExpensesUseCase,
    private val observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
) : StateHolder<ReceiptItemsState, ReceiptItemsEvent>() {

    override val tag = "ReceiptItemsStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    private val monthRange = localDateToday().let { monthMillisRange(it.year, it.month.number) }
    private var keyboardCalculator =
        KeyboardCalculator(decimalPlaces = DEFAULT_CURRENCY_DECIMAL_PLACES, currencySymbol = "")

    override fun getInitial(): ReceiptItemsState = ReceiptItemsState.Loading

    override suspend fun bootstrap(emit: suspend (DomainResult) -> Unit) {
        when (val result = consumePendingReceiptDataUseCase()) {
            is ConsumePendingReceiptDataUseCase.Result.Success -> {
                val data = result.data
                val items = data.items.mapIndexed { index, item ->
                    item.toUiModel(index, keyboardCalculator)
                }.toImmutableList()
                val parsedDateMillis = data.dateString?.let { parseDateToMillis(it) }
                val dateMillis = parsedDateMillis ?: nowMillis()
                emit(
                    ReceiptItemsResult.Initialized(
                        ReceiptItemsState.Content(
                            merchantName = data.merchantName,
                            dateMillis = dateMillis,
                            isDateAutoAssigned = parsedDateMillis == null && !data.isManualEntry,
                            items = items,
                            currency = CurrencyConfig(
                                symbol = "",
                                decimalPlaces = DEFAULT_CURRENCY_DECIMAL_PLACES,
                            ),
                            totalAmountMinorUnits = items.sumOf { it.amountMinorUnits },
                            isDateFromDifferentMonth = isDateOutsideCurrentMonth(dateMillis),
                            isManualEntry = data.isManualEntry,
                            defaultCategoryId = data.categoryId,
                            defaultSubcategoryId = data.subcategoryId,
                            pendingCurrencyCode = data.currency,
                        )
                    )
                )
            }

            is ConsumePendingReceiptDataUseCase.Result.NoPendingData -> {
                emit(ReceiptItemsResult.GoBack)
            }
        }
    }

    override fun collectWhileSubscribed(): Flow<DomainResult> = observeFormDataUseCase()

    override fun getNavigationResults(): Set<KClass<out DomainResult>> =
        setOf(
            ReceiptItemsResult.Saved::class,
            ReceiptItemsResult.GoBack::class,
        )

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is ReceiptItemsResult.Saved -> ReceiptItemsNavigation.Saved
        is ReceiptItemsResult.GoBack -> ReceiptItemsNavigation.GoBack
        else -> null
    }

    override fun getStateByResult(
        previous: ReceiptItemsState,
        result: DomainResult,
    ): ReceiptItemsState = when (result) {
        is ObserveAddExpenseFormDataResult.Success -> applyFormData(previous, result.data)

        is ReceiptItemsResult.Initialized -> result.content.withDerivedFields()

        is ReceiptItemsResult.UpdateContent -> {
            val content = previous as? ReceiptItemsState.Content ?: return previous
            val updated = result.updater(content)
            if (updated is ReceiptItemsState.Content) updated.withDerivedFields() else updated
        }

        else -> super.getStateByResult(previous, result)
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType,
    ): ReceiptItemsState = state.value.toError(errorType)

    override fun resolveEventResult(event: ReceiptItemsEvent): Flow<DomainResult> = when (event) {
        is ReceiptItemsEvent.EditItem -> {
            flowOf(updateContent { copy(editingItemId = event.itemId) })
        }

        is ReceiptItemsEvent.DoneEditingItem -> {
            flowOf(updateContent { copy(editingItemId = null) })
        }

        is ReceiptItemsEvent.ItemNameChanged -> {
            flowOf(updateContent { updateItem(event.itemId) { copy(name = event.name) } })
        }

        is ReceiptItemsEvent.ItemAmountChanged -> {
            flowOf(
                updateContent {
                    updateItem(event.itemId) {
                        val minorUnits = parseAmountInput(event.amountInput)
                        copy(amountInput = event.amountInput, amountMinorUnits = minorUnits)
                    }.withTotalRecalculated()
                }
            )
        }

        is ReceiptItemsEvent.ItemCategoryChanged -> flowOf(
            updateContent {
                updateItem(event.itemId) {
                    copy(
                        categoryId = event.categoryId,
                        categoryName = categories.firstOrNull { it.id == event.categoryId }?.name,
                        subcategoryId = null,
                        subcategoryName = null,
                    )
                }
            }
        )

        is ReceiptItemsEvent.ItemSubcategoryChanged -> flowOf(
            updateContent {
                updateItem(event.itemId) {
                    copy(
                        subcategoryId = event.subcategoryId,
                        subcategoryName = subcategories
                            .firstOrNull { it.id == event.subcategoryId }?.name,
                    )
                }
            }
        )

        is ReceiptItemsEvent.DeleteItem -> flowOf(
            updateContent {
                val updated = items.filter { it.id != event.itemId }.toImmutableList()
                copy(items = updated, totalAmountMinorUnits = updated.sumOf { it.amountMinorUnits })
            }
        )

        is ReceiptItemsEvent.DateChanged -> flowOf(
            updateContent {
                copy(
                    dateMillis = event.epochMillis,
                    isDateAutoAssigned = false,
                    showDatePicker = false,
                )
            }
        )

        is ReceiptItemsEvent.ShowDateWarningDialog -> flowOf(
            updateContent { copy(showDateWarningDialog = true) }
        )

        is ReceiptItemsEvent.DismissDateWarningDialog -> flowOf(
            updateContent { copy(showDateWarningDialog = false) }
        )

        is ReceiptItemsEvent.ShowDatePicker -> flowOf(updateContent { copy(showDatePicker = true) })
        is ReceiptItemsEvent.DismissDatePicker -> {
            flowOf(updateContent { copy(showDatePicker = false) })
        }

        is ReceiptItemsEvent.AddItem -> flowOf(
            updateContent {
                val newItem = ReceiptItemUiModel(
                    id = "manual_${items.size}",
                    name = "",
                    amountInput = "",
                    amountMinorUnits = 0L,
                    categoryId = defaultCategoryId,
                    subcategoryId = defaultSubcategoryId,
                )
                val updated = (items + newItem).toImmutableList()
                copy(items = updated, editingItemId = newItem.id)
            }
        )

        is ReceiptItemsEvent.ConfirmAll -> handleConfirmAll()
        is ReceiptItemsEvent.BackClicked -> flowOf(ReceiptItemsResult.GoBack)
        is ReceiptItemsEvent.DismissError -> flowOf(ReceiptItemsResult.GoBack)
    }

    private fun applyFormData(
        previous: ReceiptItemsState,
        data: AddExpenseFormData,
    ): ReceiptItemsState {
        keyboardCalculator = KeyboardCalculator(
            decimalPlaces = data.decimalPlaces,
            currencySymbol = data.currencySymbol,
        )
        val content = previous as? ReceiptItemsState.Content ?: return previous
        val updatedItems = content.items.map { item ->
            item.copy(
                amountInput = keyboardCalculator.formatFromMinorUnits(item.amountMinorUnits),
                categoryName = data.categories.firstOrNull { it.id == item.categoryId }?.name,
                subcategoryName = data.subcategories
                    .firstOrNull { it.id == item.subcategoryId }?.name,
            )
        }.toImmutableList()
        return content.copy(
            currency = AddExpenseStateMapper.toCurrencyConfig(data),
            items = updatedItems,
            categories = AddExpenseStateMapper.toCategoryUiModels(data),
            subcategories = AddExpenseStateMapper.toSubcategoryUiModels(data),
            defaultCategoryId = content.defaultCategoryId ?: data.categories.firstOrNull()?.id,
        ).withDerivedFields()
    }

    private fun handleConfirmAll(): Flow<DomainResult> = flow {
        emit(updateContent { copy(isSaving = true) })
        val content = state.value as? ReceiptItemsState.Content ?: run {
            emit(updateContent { copy(isSaving = false) })
            return@flow
        }
        val catId = content.defaultCategoryId ?: run {
            emit(updateContent { copy(isSaving = false) })
            return@flow
        }

        val domainItems = content.items.map { ui ->
            ReceiptExpenseItem(
                id = ui.id,
                name = ui.name,
                amountMinorUnits = ui.amountMinorUnits,
                categoryId = ui.categoryId,
                subcategoryId = ui.subcategoryId,
            )
        }

        val result = saveReceiptExpensesUseCase(
            merchantName = content.merchantName,
            currency = content.pendingCurrencyCode,
            dateEpochMillis = content.dateMillis,
            items = domainItems,
            defaultCategoryId = catId,
            dateMillis = content.dateMillis,
            conversionRate = null,
            originalCurrencyCode = null,
        )

        when (result) {
            is SaveReceiptExpensesUseCase.Result.Success -> {
                emit(ReceiptItemsResult.Saved)
            }

            is SaveReceiptExpensesUseCase.Result.Failure -> {
                emit(updateContent { copy(isSaving = false) })
            }

            else -> emit(updateContent { copy(isSaving = false) })
        }
    }

    private fun parseAmountInput(input: String): Long {
        val value = input.toDoubleOrNull() ?: return 0L
        val content = state.value as? ReceiptItemsState.Content ?: return 0L
        val multiplier = minorUnitsMultiplier(content.currency.decimalPlaces)
        return (value * multiplier).toLong()
    }

    private fun parseDateToMillis(dateString: String): Long? = runCatching {
        val localDate = kotlinx.datetime.LocalDate.parse(dateString)
        localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }.getOrNull()

    private fun ReceiptItemsState.Content.updateItem(
        itemId: String,
        transform: ReceiptItemUiModel.() -> ReceiptItemUiModel,
    ): ReceiptItemsState.Content = copy(
        items = items.map { if (it.id == itemId) it.transform() else it }.toImmutableList()
    )

    private fun ReceiptItemsState.Content.withTotalRecalculated(): ReceiptItemsState.Content =
        copy(totalAmountMinorUnits = items.sumOf { it.amountMinorUnits })

    private fun updateContent(
        updater: ReceiptItemsState.Content.() -> ReceiptItemsState,
    ): DomainResult = ReceiptItemsResult.UpdateContent(updater)

    private fun isDateOutsideCurrentMonth(dateMillis: Long): Boolean =
        dateMillis < monthRange.fromMillis || dateMillis >= monthRange.toMillis

    private fun ReceiptItemsState.Content.withDerivedFields(): ReceiptItemsState.Content =
        copy(
            isDateFromDifferentMonth = isDateOutsideCurrentMonth(dateMillis),
            editingItem = editingItemId?.let { id -> items.firstOrNull { it.id == id } },
        )

    private fun ReceiptItemsState.toError(errorType: ErrorType): ReceiptItemsState.Error =
        ReceiptItemsState.Error(
            errorType = errorType,
            merchantName = merchantName,
            dateMillis = dateMillis,
            items = items,
            currency = currency,
            totalAmountMinorUnits = totalAmountMinorUnits,
            isSaving = false,
            conversionSummary = conversionSummary,
            editingItemId = editingItemId,
            isDateAutoAssigned = isDateAutoAssigned,
            showDateWarningDialog = showDateWarningDialog,
            showDatePicker = showDatePicker,
            isDateFromDifferentMonth = isDateFromDifferentMonth,
            editingItem = editingItem,
            categories = categories,
            subcategories = subcategories,
            isManualEntry = isManualEntry,
            defaultCategoryId = defaultCategoryId,
            defaultSubcategoryId = defaultSubcategoryId,
            pendingCurrencyCode = pendingCurrencyCode,
        )
}

private fun ReceiptExpenseItem.toUiModel(
    index: Int,
    calculator: KeyboardCalculator,
): ReceiptItemUiModel = ReceiptItemUiModel(
    id = id.ifBlank { "${name}_$index" },
    name = name,
    amountInput = calculator.formatFromMinorUnits(amountMinorUnits),
    amountMinorUnits = amountMinorUnits,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
)

private sealed interface ReceiptItemsResult : DomainResult {
    data object Saved : ReceiptItemsResult
    data object GoBack : ReceiptItemsResult
    data class Initialized(val content: ReceiptItemsState.Content) : ReceiptItemsResult
    data class UpdateContent(
        val updater: ReceiptItemsState.Content.() -> ReceiptItemsState
    ) : ReceiptItemsResult
}
