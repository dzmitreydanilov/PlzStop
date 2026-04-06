package com.please.stop.app.features.categories.domain.model

sealed interface ExpenseDeletionAction {
    data class MoveToCategory(val targetCategoryId: Long) : ExpenseDeletionAction
    data object DeleteExpenses : ExpenseDeletionAction
}
