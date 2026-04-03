package com.please.stop.app.features.addexpense.presentation

sealed interface AddExpenseEvent {
    data class KeyPressed(val key: NumericKey) : AddExpenseEvent
    data class TitleChanged(val text: String) : AddExpenseEvent
    data class CategorySelected(val categoryId: Long) : AddExpenseEvent
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

    data object DismissReceiptError : AddExpenseEvent
}

sealed interface NumericKey {
    data class Digit(val value: Int) : NumericKey
    data object Decimal : NumericKey
    data object Backspace : NumericKey
}
