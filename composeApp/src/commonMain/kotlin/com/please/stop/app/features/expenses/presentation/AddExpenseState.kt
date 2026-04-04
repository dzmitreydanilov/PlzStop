package com.please.stop.app.features.expenses.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface AddExpenseState {
    val editContext: EditContext
    val currency: CurrencyConfig
    val form: ExpenseFormInput

    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<CategoryUiModel>

    @Serializable(with = ImmutableListSerializer::class)
    val subcategories: ImmutableList<SubcategoryUiModel>
    val selectedCategory: CategoryUiModel?

    @Serializable(with = ImmutableListSerializer::class)
    val titleTags: ImmutableList<String>
    val dateTime: LocalDateTime
    val showCurrencyPicker: Boolean
    val status: FormStatus
    val receipt: ReceiptState
    val conversion: ConversionState
    val currencyConversionEnabled: Boolean

    @Serializable
    data object Loading : AddExpenseState {
        override val editContext: EditContext = EditContext(isEditMode = false, existingExpenseId = null)
        override val currency: CurrencyConfig = CurrencyConfig(symbol = "", decimalPlaces = 0)
        override val form: ExpenseFormInput = ExpenseFormInput(dateEpochMillis = 0L)
        override val categories: ImmutableList<CategoryUiModel> = persistentListOf()
        override val subcategories: ImmutableList<SubcategoryUiModel> = persistentListOf()
        override val selectedCategory: CategoryUiModel? = null
        override val titleTags: ImmutableList<String> = persistentListOf()
        override val dateTime: LocalDateTime = LocalDateTime(2000, 1, 1, 0, 0)
        override val showCurrencyPicker: Boolean = false
        override val status: FormStatus = FormStatus()
        override val receipt: ReceiptState = ReceiptState()
        override val conversion: ConversionState = ConversionState()
        override val currencyConversionEnabled: Boolean = false
    }

    @Serializable
    data class Content(
        override val editContext: EditContext,
        override val currency: CurrencyConfig,
        override val form: ExpenseFormInput,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryUiModel>,
        @Serializable(with = ImmutableListSerializer::class)
        override val subcategories: ImmutableList<SubcategoryUiModel>,
        override val selectedCategory: CategoryUiModel? = null,
        @Serializable(with = ImmutableListSerializer::class)
        override val titleTags: ImmutableList<String> = persistentListOf(),
        override val dateTime: LocalDateTime = LocalDateTime(2000, 1, 1, 0, 0),
        override val showCurrencyPicker: Boolean = false,
        override val status: FormStatus,
        override val receipt: ReceiptState,
        override val conversion: ConversionState = ConversionState(),
        override val currencyConversionEnabled: Boolean = false,
    ) : AddExpenseState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        val receiptError: ReceiptError? = null,
        override val editContext: EditContext,
        override val currency: CurrencyConfig,
        override val form: ExpenseFormInput,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryUiModel>,
        @Serializable(with = ImmutableListSerializer::class)
        override val subcategories: ImmutableList<SubcategoryUiModel>,
        override val selectedCategory: CategoryUiModel? = null,
        @Serializable(with = ImmutableListSerializer::class)
        override val titleTags: ImmutableList<String> = persistentListOf(),
        override val dateTime: LocalDateTime = LocalDateTime(2000, 1, 1, 0, 0),
        override val showCurrencyPicker: Boolean = false,
        override val status: FormStatus,
        override val receipt: ReceiptState,
        override val conversion: ConversionState = ConversionState(),
        override val currencyConversionEnabled: Boolean = false,
    ) : AddExpenseState
}

@Stable
@Serializable
data class EditContext(
    val isEditMode: Boolean,
    val existingExpenseId: Long?,
    val initialForm: ExpenseFormInput? = null,
)

@Stable
@Serializable
data class CurrencyConfig(
    val code: String = "",
    val symbol: String,
    val decimalPlaces: Int,
)


@Stable
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

@Stable
@Serializable
data class FormStatus(
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val isFormValid: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
)

@Stable
@Serializable
data class ReceiptState(
    val isAnalyzing: Boolean = false,
)

@Stable
@Serializable
data class CategoryUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
)

@Stable
@Serializable
data class SubcategoryUiModel(
    val id: Long,
    val parentCategoryId: Long,
    val name: String,
    val iconKey: String,
)

@Stable
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
