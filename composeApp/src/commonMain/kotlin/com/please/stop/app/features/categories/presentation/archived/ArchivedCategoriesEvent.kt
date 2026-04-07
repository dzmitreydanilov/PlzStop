package com.please.stop.app.features.categories.presentation.archived

sealed interface ArchivedCategoriesEvent {
    data class RestoreCategoryClicked(val categoryId: Long) : ArchivedCategoriesEvent
    data object DismissError : ArchivedCategoriesEvent
}
