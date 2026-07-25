package com.please.stop.app.features.categories.presentation

sealed interface CategoriesEvent {
    data object AddCategoryClicked : CategoriesEvent
    data class ConfirmAddCategory(
        val name: String,
        val iconKey: String,
        val comment: String?,
    ) : CategoriesEvent
    data object DismissAddCategorySheet : CategoriesEvent
    data class ExpandSubcategories(val categoryId: Long) : CategoriesEvent
    data class AddSubcategoryClicked(val categoryId: Long) : CategoriesEvent
    data class ConfirmAddSubcategory(
        val categoryId: Long,
        val name: String,
        val comment: String?,
    ) : CategoriesEvent
    data object DismissAddSubcategorySheet : CategoriesEvent
    data class DeleteSubcategoryClicked(val subcategory: SubcategoryChipUiModel) : CategoriesEvent
    data class ConfirmDeleteSubcategory(val subcategoryId: Long) : CategoriesEvent
    data object DismissDeleteSubcategoryDialog : CategoriesEvent
    data class EditCategoryClicked(val category: CategoryRowUiModel) : CategoriesEvent
    data class ConfirmEditCategory(
        val id: Long,
        val name: String,
        val iconKey: String,
        val comment: String?,
    ) : CategoriesEvent
    data object DismissEditCategorySheet : CategoriesEvent
    data class ArchiveCategoryClicked(val categoryId: Long) : CategoriesEvent
    data class ConfirmArchiveCategory(val categoryId: Long) : CategoriesEvent
    data object DismissArchiveDialog : CategoriesEvent
    data object DismissError : CategoriesEvent
}
