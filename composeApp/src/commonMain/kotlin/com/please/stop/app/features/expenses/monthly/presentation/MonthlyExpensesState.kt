package com.please.stop.app.features.expenses.monthly.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.core.serialization.ImmutableSetSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable

@Serializable
sealed interface MonthlyExpensesState {

    @Serializable
    data object Loading : MonthlyExpensesState

    @Serializable
    data class Content(
        val year: Int,
        val month: Int,
        val monthLabel: String,
        val canGoToPreviousMonth: Boolean,
        val canGoToNextMonth: Boolean,
        @Serializable(with = ImmutableListSerializer::class)
        val dayGroups: ImmutableList<DayGroupUiModel>,
        val totalFormatted: String,
        val isEmpty: Boolean,
        @Serializable(with = ImmutableSetSerializer::class)
        val expandedReceiptIds: ImmutableSet<Long> = persistentSetOf(),
    ) : MonthlyExpensesState

    @Serializable
    data class Error(val errorType: ErrorType) : MonthlyExpensesState
}

@Serializable
data class DayGroupUiModel(
    val dayLabel: String,
    val dayEpochMillis: Long,
    val totalFormatted: String,
    @Serializable(with = ImmutableListSerializer::class)
    val entries: ImmutableList<ExpenseEntryUiModel>,
)

@Serializable
sealed interface ExpenseEntryUiModel {

    @Serializable
    data class Single(
        val id: Long,
        val title: String,
        val categoryName: String,
        val categoryIconKey: String,
        val amountFormatted: String,
    ) : ExpenseEntryUiModel

    @Serializable
    data class ReceiptGroup(
        val receiptId: Long,
        val merchantName: String,
        val itemCount: Int,
        val amountFormatted: String,
        @Serializable(with = ImmutableListSerializer::class)
        val expenses: ImmutableList<Single>,
    ) : ExpenseEntryUiModel
}
