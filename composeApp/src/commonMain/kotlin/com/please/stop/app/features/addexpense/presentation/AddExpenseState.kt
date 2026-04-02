package com.please.stop.app.features.addexpense.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.addexpense.domain.model.ReceiptError
import com.please.stop.app.core.serialization.ImmutableListSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface AddExpenseState {

    @Serializable
    data object Loading : AddExpenseState

    @Serializable
    data class Content(
        val isEditMode: Boolean = false,
        val existingExpenseId: Long? = null,
        val currencySymbol: String = "$",
        val decimalPlaces: Int = 2,
        val amountInput: String = "",
        val title: String = "",
        val selectedCategoryId: Long? = null,
        @Serializable(with = ImmutableListSerializer::class)
        val categories: ImmutableList<CategoryUiModel> = persistentListOf(),
        val dateEpochMillis: Long = 0L,
        val notes: String = "",
        val isSaving: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val showDeleteDialog: Boolean = false,
        val isFormValid: Boolean = false,
        val hasUnsavedChanges: Boolean = false,
        val errorType: ErrorType? = null,
        val isAnalyzingReceipt: Boolean = false,
        val receiptError: ReceiptError? = null,
    ) : AddExpenseState

    @Serializable
    data class Error(val errorType: ErrorType) : AddExpenseState
}

@Stable
@Serializable
data class CategoryUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
)
