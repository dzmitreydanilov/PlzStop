package com.please.stop.app.features.expenses.receiptitems.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.expenses.domain.model.ExpenseCategory
import com.please.stop.app.features.expenses.domain.model.ExpenseSubcategory
import com.please.stop.app.features.expenses.domain.model.ReceiptExpenseItem
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataResult
import com.please.stop.app.features.expenses.domain.usecase.ObserveAddExpenseFormDataUseCase
import com.please.stop.app.features.expenses.domain.usecase.SaveReceiptExpensesUseCase
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.features.expenses.presentation.CurrencyConfig
import com.please.stop.app.features.expenses.presentation.KeyboardCalculator
import com.please.stop.app.features.expenses.presentation.SubcategoryUiModel
import com.please.stop.app.features.expenses.receiptitems.ReceiptItemsArgsHolder
import com.please.stop.app.navigation.routes.ReceiptItemsRoute
import com.please.stop.app.utils.date.nowMillis
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.math.pow
import kotlin.reflect.KClass
import com.please.stop.app.core.models.domain.Result as DomainResult

class ReceiptItemsStateHolder(
    private val route: ReceiptItemsRoute,
    private val argsHolder: ReceiptItemsArgsHolder,
    private val saveReceiptExpensesUseCase: SaveReceiptExpensesUseCase,
    private val observeFormDataUseCase: ObserveAddExpenseFormDataUseCase,
) : StateHolder<ReceiptItemsState, ReceiptItemsEvent>() {

    override val tag = "ReceiptItemsStateHolder"

    private var keyboardCalculator = KeyboardCalculator(decimalPlaces = 2, currencySymbol = "")
    private var decimalPlaces: Int = 2
    private var defaultCategoryId: Long? = null
    private var formCurrencyCode: String = ""
    private var formCurrencySymbol: String = ""
    private var categories: List<ExpenseCategory> = emptyList()
    private var subcategories: List<ExpenseSubcategory> = emptyList()

    override fun getInitial(): ReceiptItemsState {
        val items = argsHolder.pendingItems.mapIndexed { index, item ->
            item.toUiModel(index, keyboardCalculator)
        }.toImmutableList()
        val parsedDateMillis = route.dateString?.let { parseDateToMillis(it) }
        return ReceiptItemsState.Content(
            merchantName = route.merchantName,
            dateMillis = parsedDateMillis ?: nowMillis(),
            isDateAutoAssigned = parsedDateMillis == null,
            items = items,
            currency = CurrencyConfig(symbol = "", decimalPlaces = 2),
            totalAmountMinorUnits = items.sumOf { it.amountMinorUnits },
        )
    }

    override fun collectFlowsOnInit(): Flow<DomainResult> = observeFormDataUseCase()

    override fun getNavigationResults(): Set<KClass<out DomainResult>> =
        setOf(
            ReceiptItemsResult.PopTwice::class,
            ReceiptItemsResult.PopOnce::class,
            ReceiptItemsResult.GoBack::class
        )

    override fun getNavigationByResult(result: DomainResult): Navigation? = when (result) {
        is ReceiptItemsResult.PopTwice -> ReceiptItemsNavigation.PopTwice
        is ReceiptItemsResult.PopOnce -> ReceiptItemsNavigation.PopOnce
        is ReceiptItemsResult.GoBack -> ReceiptItemsNavigation.GoBack
        else -> null
    }

    override fun getStateByResult(
        previous: ReceiptItemsState,
        result: DomainResult,
    ): ReceiptItemsState = when (result) {
        is ObserveAddExpenseFormDataResult.Success -> {
            val data = result.data
            if (defaultCategoryId == null) {
                defaultCategoryId = data.categories.firstOrNull()?.id
            }
            decimalPlaces = data.decimalPlaces
            keyboardCalculator = KeyboardCalculator(
                decimalPlaces = data.decimalPlaces,
                currencySymbol = data.currencySymbol,
            )
            formCurrencyCode = data.currencyCode
            formCurrencySymbol = data.currencySymbol
            categories = data.categories
            subcategories = data.subcategories
            val content = previous as? ReceiptItemsState.Content ?: return previous
            val updatedItems = content.items.map { item ->
                item.copy(
                    amountInput = keyboardCalculator.formatFromMinorUnits(item.amountMinorUnits),
                    categoryName = data.categories.firstOrNull { it.id == item.categoryId }?.name,
                    subcategoryName = data.subcategories.firstOrNull { it.id == item.subcategoryId }?.name,
                )
            }.toImmutableList()
            val categoryUiModels =
                data.categories.map { CategoryUiModel(it.id, it.name, it.iconKey) }
                    .toImmutableList()
            val subcategoryUiModels = data.subcategories.map {
                SubcategoryUiModel(
                    it.id,
                    it.parentCategoryId,
                    it.name,
                    it.iconKey
                )
            }.toImmutableList()
            content.copy(
                currency = CurrencyConfig(
                    code = data.currencyCode,
                    symbol = data.currencySymbol,
                    decimalPlaces = data.decimalPlaces,
                ),
                items = updatedItems,
                categories = categoryUiModels,
                subcategories = subcategoryUiModels,
            )
        }

        is ReceiptItemsResult.UpdateContent -> {
            val content = previous as? ReceiptItemsState.Content ?: return previous
            result.updater(content)
        }

        else -> super.getStateByResult(previous, result)
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType
    ): ReceiptItemsState = ReceiptItemsState.Error(errorType)

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

        is ReceiptItemsEvent.ItemCategoryChanged -> flowOf(updateContent {
            updateItem(event.itemId) {
                copy(
                    categoryId = event.categoryId,
                    categoryName = categories.firstOrNull { it.id == event.categoryId }?.name,
                    subcategoryId = null,
                    subcategoryName = null,
                )
            }
        })

        is ReceiptItemsEvent.ItemSubcategoryChanged -> flowOf(updateContent {
            updateItem(event.itemId) {
                copy(
                    subcategoryId = event.subcategoryId,
                    subcategoryName = subcategories.firstOrNull { it.id == event.subcategoryId }?.name,
                )
            }
        })

        is ReceiptItemsEvent.DeleteItem -> flowOf(updateContent {
            val updated = items.filter { it.id != event.itemId }.toImmutableList()
            copy(items = updated, totalAmountMinorUnits = updated.sumOf { it.amountMinorUnits })
        })

        is ReceiptItemsEvent.DateChanged -> flowOf(updateContent {
            copy(dateMillis = event.epochMillis, isDateAutoAssigned = false, showDatePicker = false)
        })

        is ReceiptItemsEvent.ShowDateWarningDialog -> flowOf(updateContent {
            copy(showDateWarningDialog = true)
        })

        is ReceiptItemsEvent.DismissDateWarningDialog -> flowOf(updateContent {
            copy(showDateWarningDialog = false)
        })

        is ReceiptItemsEvent.ShowDatePicker -> flowOf(updateContent { copy(showDatePicker = true) })
        is ReceiptItemsEvent.DismissDatePicker -> flowOf(updateContent { copy(showDatePicker = false) })
        is ReceiptItemsEvent.AddItem -> flowOf(updateContent {
            val newItem = ReceiptItemUiModel(
                id = "manual_${items.size}",
                name = "",
                amountInput = keyboardCalculator.formatFromMinorUnits(0L),
                amountMinorUnits = 0L,
                categoryId = route.categoryId ?: defaultCategoryId,
                subcategoryId = route.subcategoryId,
            )
            val updated = (items + newItem).toImmutableList()
            copy(items = updated, editingItemId = newItem.id)
        })

        is ReceiptItemsEvent.ConfirmAll -> handleConfirmAll()
        is ReceiptItemsEvent.BackClicked -> flowOf(ReceiptItemsResult.GoBack)
    }

    private fun handleConfirmAll(): Flow<DomainResult> = flow {
        emit(updateContent { copy(isSaving = true) })
        val content = state.value as? ReceiptItemsState.Content ?: run {
            emit(updateContent { copy(isSaving = false) })
            return@flow
        }
        val catId = defaultCategoryId ?: route.categoryId ?: run {
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
            merchantName = route.merchantName,
            currency = route.currency,
            dateEpochMillis = content.dateMillis,
            items = domainItems,
            defaultCategoryId = catId,
            dateMillis = content.dateMillis,
            conversionRate = null,
            originalCurrencyCode = null,
        )

        when (result) {
            is SaveReceiptExpensesUseCase.Result.Success -> {
                val popResult = if (route.isManualEntry) {
                    ReceiptItemsResult.PopOnce
                } else {
                    ReceiptItemsResult.PopTwice
                }
                emit(popResult)
            }

            is SaveReceiptExpensesUseCase.Result.Failure -> emit(updateContent { copy(isSaving = false) })
            else -> emit(updateContent { copy(isSaving = false) })
        }
    }

    private fun parseAmountInput(input: String): Long {
        val value = input.toDoubleOrNull() ?: return 0L
        val multiplier = 10.0.pow(decimalPlaces).toLong()
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
    data object PopTwice : ReceiptItemsResult
    data object PopOnce : ReceiptItemsResult
    data object GoBack : ReceiptItemsResult
    data class UpdateContent(
        val updater: ReceiptItemsState.Content.() -> ReceiptItemsState
    ) : ReceiptItemsResult
}
