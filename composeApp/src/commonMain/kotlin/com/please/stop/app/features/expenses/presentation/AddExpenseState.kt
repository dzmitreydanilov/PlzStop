package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class AddExpenseState(
    val editContext: EditContext,
    val currency: CurrencyConfig,
    val form: ExpenseFormInput,
    val initialCategoryId: Long?,
    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<CategoryUiModel>,
    @Serializable(with = ImmutableListSerializer::class)
    val subcategories: ImmutableList<SubcategoryUiModel>,
    @Serializable(with = ImmutableListSerializer::class)
    val filteredSubcategories: ImmutableList<SubcategoryUiModel> = persistentListOf(),
    @Serializable(with = ImmutableListSerializer::class)
    val frequentSubcategories: ImmutableList<SubcategoryUiModel> = persistentListOf(),
    val selectedCategory: CategoryUiModel? = null,
    @Serializable(with = ImmutableListSerializer::class)
    val titleTags: ImmutableList<String> = persistentListOf(),
    val dateTime: LocalDateTime = LocalDateTime(2000, 1, 1, 0, 0),
    val showCurrencyPicker: Boolean = false,
    val status: FormStatus = FormStatus(),
    val receipt: ReceiptState = ReceiptState(),
    val conversion: ConversionState = ConversionState(),
    val currencyConversionEnabled: Boolean = false,
    val errorOverlay: ErrorOverlay? = null,
)

@Serializable
data class ErrorOverlay(
    val errorType: ErrorType,
    val receiptError: ReceiptError? = null,
)

@Serializable
data class EditContext(
    val isEditMode: Boolean,
    val existingExpenseId: Long?,
    val initialForm: ExpenseFormInput? = null,
)

@Serializable
data class CurrencyConfig(
    val code: String = "",
    val symbol: String,
    val decimalPlaces: Int,
)


@Serializable
data class ExpenseFormInput(
    val amountInput: String = "",
    val amountDisplayExpression: String = "",
    val isInExpressionMode: Boolean = false,
    val title: String = "",
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val dateEpochMillis: Long,
    val notes: String = "",
)

@Serializable
data class FormStatus(
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val isFormValid: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

@Serializable
data class ReceiptState(
    val isAnalyzing: Boolean = false,
)

@Serializable
data class CategoryUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
)

@Serializable
data class SubcategoryUiModel(
    val id: Long,
    val parentCategoryId: Long,
    val name: String,
    val iconKey: String,
    val frequentRank: Int? = null,
)

@Serializable
data class ConversionState(
    val isLoading: Boolean = false,
    val rate: Double? = null,
    val fetchedRate: Double? = null,
    val isManualOverride: Boolean = false,
    val showRateEditSheet: Boolean = false,
    val rateEditInput: String = "",
    val convertedAmountMinorUnits: Long? = null,
    val defaultCurrencyCode: String = "",
    val defaultCurrencySymbol: String = "",
    val saveInOriginalCurrency: Boolean = false,
    val hasFetchError: Boolean = false,
)
