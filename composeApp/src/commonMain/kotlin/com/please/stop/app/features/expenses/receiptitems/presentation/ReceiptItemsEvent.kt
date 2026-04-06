package com.please.stop.app.features.expenses.receiptitems.presentation

sealed interface ReceiptItemsEvent {
    data class EditItem(val itemId: String) : ReceiptItemsEvent
    data class ItemNameChanged(val itemId: String, val name: String) : ReceiptItemsEvent
    data class ItemAmountChanged(val itemId: String, val amountInput: String) : ReceiptItemsEvent
    data class ItemCategoryChanged(val itemId: String, val categoryId: Long) : ReceiptItemsEvent
    data class ItemSubcategoryChanged(val itemId: String, val subcategoryId: Long?) : ReceiptItemsEvent
    data class DoneEditingItem(val itemId: String) : ReceiptItemsEvent
    data class DeleteItem(val itemId: String) : ReceiptItemsEvent
    data class DateChanged(val epochMillis: Long) : ReceiptItemsEvent
    data object ShowDateWarningDialog : ReceiptItemsEvent
    data object DismissDateWarningDialog : ReceiptItemsEvent
    data object ShowDatePicker : ReceiptItemsEvent
    data object DismissDatePicker : ReceiptItemsEvent
    data object ConfirmAll : ReceiptItemsEvent
    data object AddItem : ReceiptItemsEvent
    data object BackClicked : ReceiptItemsEvent
    data object DismissError : ReceiptItemsEvent
}
