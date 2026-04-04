package com.please.stop.app.features.expenses.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
    val status: FormStatus
    val receipt: ReceiptState

    @Serializable
    data object Loading : AddExpenseState {
        override val editContext: EditContext = EditContext(isEditMode = false, existingExpenseId = null)
        override val currency: CurrencyConfig = CurrencyConfig(symbol = "", decimalPlaces = 0)
        override val form: ExpenseFormInput = ExpenseFormInput(dateEpochMillis = 0L)
        override val categories: ImmutableList<CategoryUiModel> = persistentListOf()
        override val subcategories: ImmutableList<SubcategoryUiModel> = persistentListOf()
        override val status: FormStatus = FormStatus()
        override val receipt: ReceiptState = ReceiptState()
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
        override val status: FormStatus,
        override val receipt: ReceiptState,
    ) : AddExpenseState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        override val editContext: EditContext,
        override val currency: CurrencyConfig,
        override val form: ExpenseFormInput,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryUiModel>,
        @Serializable(with = ImmutableListSerializer::class)
        override val subcategories: ImmutableList<SubcategoryUiModel>,
        override val status: FormStatus,
        override val receipt: ReceiptState,
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
    val error: ReceiptError? = null,
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
