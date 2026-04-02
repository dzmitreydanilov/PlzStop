package com.please.stop.app.features.addexpense.presentation

import com.please.stop.app.core.models.presentation.Navigation

internal sealed interface AddExpenseNavigation : Navigation {
    data object GoBack : AddExpenseNavigation
}
