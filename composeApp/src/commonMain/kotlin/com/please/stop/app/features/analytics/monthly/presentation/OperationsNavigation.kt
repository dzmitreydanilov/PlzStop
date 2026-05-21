package com.please.stop.app.features.analytics.monthly.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface OperationsNavigation : Navigation {
    data class OpenEditExpense(val expenseId: Long) : OperationsNavigation
}
