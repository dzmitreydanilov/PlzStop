package com.please.stop.app.features.expenses.receiptitems.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.features.expenses.presentation.CurrencyConfig
import com.please.stop.app.features.expenses.presentation.SubcategoryUiModel
import com.please.stop.app.utils.date.nowMillis
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
sealed interface ReceiptItemsState {
    val merchantName: String?
    val dateMillis: Long
    @Serializable(with = ImmutableListSerializer::class)
    val items: ImmutableList<ReceiptItemUiModel>
    val currency: CurrencyConfig
    val totalAmountMinorUnits: Long
    val isSaving: Boolean
    val conversionSummary: ConversionSummary?
    val editingItemId: String?
    val isDateAutoAssigned: Boolean
    val showDateWarningDialog: Boolean
    val showDatePicker: Boolean
    val isDateFromDifferentMonth: Boolean
    val editingItem: ReceiptItemUiModel?
    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<CategoryUiModel>
    @Serializable(with = ImmutableListSerializer::class)
    val subcategories: ImmutableList<SubcategoryUiModel>
    val isManualEntry: Boolean
    val defaultCategoryId: Long?
    val defaultSubcategoryId: Long?
    val pendingCurrencyCode: String?

    @Serializable
    data object Loading : ReceiptItemsState {
        override val merchantName: String? = null
        override val dateMillis: Long = nowMillis()
        override val items: ImmutableList<ReceiptItemUiModel> = persistentListOf()
        override val currency: CurrencyConfig = CurrencyConfig(symbol = "", decimalPlaces = 0)
        override val totalAmountMinorUnits: Long = 0L
        override val isSaving: Boolean = false
        override val conversionSummary: ConversionSummary? = null
        override val editingItemId: String? = null
        override val isDateAutoAssigned: Boolean = false
        override val showDateWarningDialog: Boolean = false
        override val showDatePicker: Boolean = false
        override val isDateFromDifferentMonth: Boolean = false
        override val editingItem: ReceiptItemUiModel? = null
        override val categories: ImmutableList<CategoryUiModel> = persistentListOf()
        override val subcategories: ImmutableList<SubcategoryUiModel> = persistentListOf()
        override val isManualEntry: Boolean = false
        override val defaultCategoryId: Long? = null
        override val defaultSubcategoryId: Long? = null
        override val pendingCurrencyCode: String? = null
    }

    @Serializable
    data class Content(
        override val merchantName: String?,
        override val dateMillis: Long,
        @Serializable(with = ImmutableListSerializer::class)
        override val items: ImmutableList<ReceiptItemUiModel>,
        override val currency: CurrencyConfig,
        override val totalAmountMinorUnits: Long,
        override val isSaving: Boolean = false,
        override val conversionSummary: ConversionSummary? = null,
        override val editingItemId: String? = null,
        override val isDateAutoAssigned: Boolean = false,
        override val showDateWarningDialog: Boolean = false,
        override val showDatePicker: Boolean = false,
        override val isDateFromDifferentMonth: Boolean = false,
        override val editingItem: ReceiptItemUiModel? = null,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryUiModel> = persistentListOf(),
        @Serializable(with = ImmutableListSerializer::class)
        override val subcategories: ImmutableList<SubcategoryUiModel> = persistentListOf(),
        override val isManualEntry: Boolean = false,
        override val defaultCategoryId: Long? = null,
        override val defaultSubcategoryId: Long? = null,
        override val pendingCurrencyCode: String? = null,
    ) : ReceiptItemsState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        override val merchantName: String?,
        override val dateMillis: Long,
        @Serializable(with = ImmutableListSerializer::class)
        override val items: ImmutableList<ReceiptItemUiModel>,
        override val currency: CurrencyConfig,
        override val totalAmountMinorUnits: Long,
        override val isSaving: Boolean,
        override val conversionSummary: ConversionSummary?,
        override val editingItemId: String?,
        override val isDateAutoAssigned: Boolean,
        override val showDateWarningDialog: Boolean,
        override val showDatePicker: Boolean,
        override val isDateFromDifferentMonth: Boolean,
        override val editingItem: ReceiptItemUiModel?,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryUiModel>,
        @Serializable(with = ImmutableListSerializer::class)
        override val subcategories: ImmutableList<SubcategoryUiModel>,
        override val isManualEntry: Boolean,
        override val defaultCategoryId: Long?,
        override val defaultSubcategoryId: Long?,
        override val pendingCurrencyCode: String?,
    ) : ReceiptItemsState
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
