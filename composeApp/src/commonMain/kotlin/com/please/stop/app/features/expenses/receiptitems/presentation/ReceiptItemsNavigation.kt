package com.please.stop.app.features.expenses.receiptitems.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface ReceiptItemsNavigation : Navigation {
    data object Saved : ReceiptItemsNavigation
    data object GoBack : ReceiptItemsNavigation
}
