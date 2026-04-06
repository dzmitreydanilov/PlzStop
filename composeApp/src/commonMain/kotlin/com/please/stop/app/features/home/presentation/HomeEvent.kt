package com.please.stop.app.features.home.presentation

sealed interface HomeEvent {
    data class CategoryClicked(val categoryId: Long) : HomeEvent
    data object AddExpenseClicked : HomeEvent
    data object AddCategoryClicked : HomeEvent
    data class ConfirmAddCategory(val name: String) : HomeEvent
    data object DismissAddCategorySheet : HomeEvent
    data object DismissError : HomeEvent
    data object ProfileClicked : HomeEvent
    data object TotalSpentCardClick : HomeEvent
}
