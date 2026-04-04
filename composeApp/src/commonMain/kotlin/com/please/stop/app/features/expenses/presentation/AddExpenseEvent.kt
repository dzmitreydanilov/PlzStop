package com.please.stop.app.features.expenses.presentation

sealed interface AddExpenseEvent {
    data class KeyPressed(val key: NumericKey) : AddExpenseEvent
    data class TitleChanged(val text: String) : AddExpenseEvent
    data class CategorySelected(val categoryId: Long) : AddExpenseEvent
    data class SubcategorySelected(val subcategoryId: Long?) : AddExpenseEvent
    data class DateChanged(val epochMillis: Long) : AddExpenseEvent
    data class TimeChanged(val hour: Int, val minute: Int) : AddExpenseEvent
    data class NotesChanged(val text: String) : AddExpenseEvent
    data object SaveClicked : AddExpenseEvent
    data object DeleteClicked : AddExpenseEvent
    data object ConfirmDelete : AddExpenseEvent
    data object DismissDeleteDialog : AddExpenseEvent
    data object BackClicked : AddExpenseEvent
    data object ConfirmDiscard : AddExpenseEvent
    data object DismissDiscardDialog : AddExpenseEvent
    data object DismissError : AddExpenseEvent
    data class ReceiptScanned(val imageBytes: ByteArray) : AddExpenseEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ReceiptScanned

            return imageBytes.contentEquals(other.imageBytes)
        }

        override fun hashCode(): Int {
            return imageBytes.contentHashCode()
        }
    }

    data object DismissDatePicker : AddExpenseEvent
    data object DismissCurrencyPicker : AddExpenseEvent
    data class ExpenseCurrencySelected(
        val currency: com.please.stop.app.features.onboarding.domain.model.Currency,
    ) : AddExpenseEvent

    data object ShowRateOverrideSheet : AddExpenseEvent
    data object DismissRateOverrideSheet : AddExpenseEvent
    data class RateOverrideInputChanged(val input: String) : AddExpenseEvent
    data object ConfirmRateOverride : AddExpenseEvent
    data object ResetToFetchedRate : AddExpenseEvent
    data object ToggleSaveInOriginalCurrency : AddExpenseEvent
}

enum class KeyboardOperator {
    ADD, SUBTRACT, MULTIPLY, DIVIDE
}

sealed interface NumericKey {
    data class Digit(val value: Int) : NumericKey
    data object Decimal : NumericKey
    data object Backspace : NumericKey
    data class Operator(val op: KeyboardOperator) : NumericKey
    data object Equals : NumericKey
    data object Calendar : NumericKey
    data object CurrencySymbol : NumericKey
}
