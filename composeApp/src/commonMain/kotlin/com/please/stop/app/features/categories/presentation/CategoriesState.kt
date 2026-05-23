package com.please.stop.app.features.categories.presentation

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
sealed interface CategoriesState {

    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<CategoryRowUiModel>
    val showAddCategorySheet: Boolean
    val addSubcategoryForCategoryId: Long?
    val editingCategory: CategoryRowUiModel?
    val archiveDialog: CategoryArchiveDialog?
    val subcategoryToDelete: SubcategoryChipUiModel?

    @Serializable
    data object Loading : CategoriesState {
        override val categories: ImmutableList<CategoryRowUiModel> = persistentListOf()
        override val showAddCategorySheet: Boolean = false
        override val addSubcategoryForCategoryId: Long? = null
        override val editingCategory: CategoryRowUiModel? = null
        override val archiveDialog: CategoryArchiveDialog? = null
        override val subcategoryToDelete: SubcategoryChipUiModel? = null
    }

    @Serializable
    data class Content(
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryRowUiModel>,
        override val showAddCategorySheet: Boolean,
        override val addSubcategoryForCategoryId: Long?,
        override val editingCategory: CategoryRowUiModel?,
        override val archiveDialog: CategoryArchiveDialog? = null,
        override val subcategoryToDelete: SubcategoryChipUiModel? = null,
    ) : CategoriesState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryRowUiModel>,
        override val showAddCategorySheet: Boolean,
        override val addSubcategoryForCategoryId: Long?,
        override val editingCategory: CategoryRowUiModel?,
        override val archiveDialog: CategoryArchiveDialog? = null,
        override val subcategoryToDelete: SubcategoryChipUiModel? = null,
    ) : CategoriesState
}

@Serializable
data class CategoryArchiveDialog(val categoryId: Long, val categoryName: String)

/**
 * @param subcategories `null` when subcategories feature is disabled,
 *   empty list when enabled but none exist yet.
 */
@Serializable
data class CategoryRowUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
    val comment: String?,
    @Serializable(with = ImmutableListSerializer::class)
    val subcategories: ImmutableList<SubcategoryChipUiModel>?,
)

@Serializable
data class SubcategoryChipUiModel(
    val id: Long,
    val name: String,
    val comment: String?,
)
