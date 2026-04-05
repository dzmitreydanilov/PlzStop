package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.navigation.routes.ReceiptItemsRoute

internal sealed interface AddExpenseNavigation : Navigation {
    data object GoBack : AddExpenseNavigation
    data class OpenReceiptItems(val route: ReceiptItemsRoute) : AddExpenseNavigation
}
