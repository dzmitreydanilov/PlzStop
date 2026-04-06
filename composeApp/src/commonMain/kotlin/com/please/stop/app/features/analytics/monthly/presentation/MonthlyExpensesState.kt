package com.please.stop.app.features.analytics.monthly.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.core.serialization.ImmutableSetSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class MonthlyWindowState(
    @Serializable(with = ImmutableListSerializer::class)
    val pages: ImmutableList<MonthPageEntry> = persistentListOf(),
    val deleteConfirmExpenseId: Long? = null,
) {
    fun pageStateFor(year: Int, month: Int): MonthPageState =
        pages.find { it.year == year && it.month == month }?.pageState ?: MonthPageState.Loading
}

@Serializable
data class MonthPageEntry(
    val year: Int,
    val month: Int,
    val pageState: MonthPageState,
)

@Serializable
@Stable
sealed interface MonthPageState {
    val dayGroups: ImmutableList<DayGroupUiModel>
    val totalFormatted: String?
    val isEmpty: Boolean
    @Serializable(with = ImmutableSetSerializer::class)
    val expandedReceiptIds: ImmutableSet<Long>

    @Serializable
    data object Loading : MonthPageState {
        override val dayGroups: ImmutableList<DayGroupUiModel> = persistentListOf()
        override val totalFormatted: String? = null
        override val isEmpty: Boolean = true
        override val expandedReceiptIds: ImmutableSet<Long> = persistentSetOf()
    }

    @Serializable
    data class Content(
        @Serializable(with = ImmutableListSerializer::class)
        override val dayGroups: ImmutableList<DayGroupUiModel>,
        override val totalFormatted: String,
        override val isEmpty: Boolean,
        @Serializable(with = ImmutableSetSerializer::class)
        override val expandedReceiptIds: ImmutableSet<Long> = persistentSetOf(),
    ) : MonthPageState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        @Serializable(with = ImmutableListSerializer::class)
        override val dayGroups: ImmutableList<DayGroupUiModel>,
        override val totalFormatted: String?,
        override val isEmpty: Boolean,
        @Serializable(with = ImmutableSetSerializer::class)
        override val expandedReceiptIds: ImmutableSet<Long>,
    ) : MonthPageState
}

@Serializable
@Stable
data class DayGroupUiModel(
    val dayLabel: String,
    val dayEpochMillis: Long,
    val totalFormatted: String,
    @Serializable(with = ImmutableListSerializer::class)
    val entries: ImmutableList<ExpenseEntryUiModel>,
)

@Serializable
@Stable
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
        val merchantName: String?,
        val itemCount: Int,
        val amountFormatted: String,
        @Serializable(with = ImmutableListSerializer::class)
        val expenses: ImmutableList<Single>,
    ) : ExpenseEntryUiModel
}
