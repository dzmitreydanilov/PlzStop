package com.please.stop.app.features.expenses.receiptitems.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.features.expenses.presentation.CurrencyConfig
import com.please.stop.app.features.expenses.presentation.SubcategoryUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
sealed interface ReceiptItemsState {
    @Serializable
    data class Content(
        val merchantName: String?,
        val dateMillis: Long,
        @Serializable(with = ImmutableListSerializer::class)
        val items: ImmutableList<ReceiptItemUiModel>,
        val currency: CurrencyConfig,
        val totalAmountMinorUnits: Long,
        val isSaving: Boolean = false,
        val conversionSummary: ConversionSummary? = null,
        val editingItemId: String? = null,
        val isDateAutoAssigned: Boolean = false,
        val showDateWarningDialog: Boolean = false,
        val showDatePicker: Boolean = false,
        @Serializable(with = ImmutableListSerializer::class)
        val categories: ImmutableList<CategoryUiModel> = persistentListOf(),
        @Serializable(with = ImmutableListSerializer::class)
        val subcategories: ImmutableList<SubcategoryUiModel> = persistentListOf(),
    ) : ReceiptItemsState

    @Serializable
    data class Error(val errorType: ErrorType) : ReceiptItemsState
}

@Serializable
data class ReceiptItemUiModel(
    val id: String,
    val name: String,
    val amountInput: String,
    val amountMinorUnits: Long,
    val categoryId: Long?,
    val subcategoryId: Long?,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
)

@Serializable
data class ConversionSummary(
    val rate: Double,
    val originalCurrencyCode: String,
    val convertedTotalMinorUnits: Long,
    val defaultCurrencySymbol: String,
)
