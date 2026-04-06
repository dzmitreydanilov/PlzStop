package com.please.stop.app.features.categories.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface CategoriesState {

    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<CategoryRowUiModel>
    val showAddCategorySheet: Boolean
    val addSubcategoryForCategoryId: Long?
    val editingCategory: CategoryRowUiModel?
    val deletionDialog: CategoryDeletionDialog?
    val subcategoryToDelete: SubcategoryChipUiModel?
    val successMessage: String?

    @Serializable
    data object Loading : CategoriesState {
        override val categories: ImmutableList<CategoryRowUiModel> = persistentListOf()
        override val showAddCategorySheet: Boolean = false
        override val addSubcategoryForCategoryId: Long? = null
        override val editingCategory: CategoryRowUiModel? = null
        override val deletionDialog: CategoryDeletionDialog? = null
        override val subcategoryToDelete: SubcategoryChipUiModel? = null
        override val successMessage: String? = null
    }

    @Serializable
    data class Content(
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryRowUiModel>,
        override val showAddCategorySheet: Boolean,
        override val addSubcategoryForCategoryId: Long?,
        override val editingCategory: CategoryRowUiModel?,
        override val deletionDialog: CategoryDeletionDialog? = null,
        override val subcategoryToDelete: SubcategoryChipUiModel? = null,
        override val successMessage: String? = null,
    ) : CategoriesState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryRowUiModel>,
        override val showAddCategorySheet: Boolean,
        override val addSubcategoryForCategoryId: Long?,
        override val editingCategory: CategoryRowUiModel?,
        override val deletionDialog: CategoryDeletionDialog? = null,
        override val subcategoryToDelete: SubcategoryChipUiModel? = null,
        override val successMessage: String? = null,
    ) : CategoriesState
}

@Serializable
sealed interface CategoryDeletionDialog {
    val categoryId: Long

    @Serializable
    data class SimpleConfirm(override val categoryId: Long) : CategoryDeletionDialog

    @Serializable
    data class WithExpenses(
        override val categoryId: Long,
        val expenseCount: Int,
        @Serializable(with = ImmutableListSerializer::class)
        val availableTargets: ImmutableList<CategoryTargetUiModel>,
    ) : CategoryDeletionDialog
}

@Stable
@Serializable
data class CategoryTargetUiModel(val id: Long, val name: String, val iconKey: String)

/**
 * @param subcategories `null` when subcategories feature is disabled,
 *   empty list when enabled but none exist yet.
 */
@Stable
@Serializable
data class CategoryRowUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
    val comment: String?,
    @Serializable(with = ImmutableListSerializer::class)
    val subcategories: ImmutableList<SubcategoryChipUiModel>?,
)

@Stable
@Serializable
data class SubcategoryChipUiModel(
    val id: Long,
    val name: String,
    val comment: String?,
)
