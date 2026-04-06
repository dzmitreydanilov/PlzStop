package com.please.stop.app.features.categories.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.features.categories.domain.model.CategoryWithSubcategories
import com.please.stop.app.features.categories.domain.model.ExpenseDeletionAction
import com.please.stop.app.features.categories.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.AddSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.DeleteCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.DeleteSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.ObserveCategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.UpdateCategoryUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import com.please.stop.app.core.models.domain.Result as DomainResult

class CategoriesStateHolder(
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val addSubcategoryUseCase: AddSubcategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val deleteSubcategoryUseCase: DeleteSubcategoryUseCase,
) : StateHolder<CategoriesState, CategoriesEvent>() {

    override val tag = "CategoriesStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): CategoriesState = CategoriesState.Loading

    override fun collectFlowsOnInit(): Flow<DomainResult> = observeCategoriesUseCase()

    @Suppress("CyclomaticComplexMethod")
    override fun resolveEventResult(event: CategoriesEvent): Flow<DomainResult> = when (event) {
        CategoriesEvent.AddCategoryClicked -> flowOf(CategoriesResult.ShowAddCategorySheet)
        is CategoriesEvent.ConfirmAddCategory -> handleAddCategory(event)
        CategoriesEvent.DismissAddCategorySheet -> flowOf(CategoriesResult.HideAddCategorySheet)
        is CategoriesEvent.AddSubcategoryClicked -> {
            flowOf(CategoriesResult.ShowAddSubcategorySheet(event.categoryId))
        }

        is CategoriesEvent.ConfirmAddSubcategory -> handleAddSubcategory(event)
        CategoriesEvent.DismissAddSubcategorySheet -> {
            flowOf(CategoriesResult.HideAddSubcategorySheet)
        }

        is CategoriesEvent.EditCategoryClicked -> {
            flowOf(CategoriesResult.ShowEditCategorySheet(event.category))
        }

        is CategoriesEvent.ConfirmEditCategory -> handleEditCategory(event)
        CategoriesEvent.DismissEditCategorySheet -> flowOf(CategoriesResult.HideEditCategorySheet)
        is CategoriesEvent.DeleteCategoryClicked -> handleCheckDeleteCategory(event.categoryId)
        is CategoriesEvent.ConfirmDeleteCategory -> handleDeleteEmpty(event.categoryId)
        is CategoriesEvent.ConfirmDeleteCategoryMoveExpenses -> {
            handleDeleteWithAction(
                event.categoryId,
                ExpenseDeletionAction.MoveToCategory(event.targetCategoryId)
            )
        }

        is CategoriesEvent.ConfirmDeleteCategoryDeleteExpenses -> {
            handleDeleteWithAction(event.categoryId, ExpenseDeletionAction.DeleteExpenses)
        }

        CategoriesEvent.DismissDeletionDialog -> flowOf(CategoriesResult.HideDeletionDialog)
        is CategoriesEvent.DeleteSubcategoryClicked -> {
            flowOf(CategoriesResult.ShowDeleteSubcategoryDialog(event.subcategory))
        }

        is CategoriesEvent.ConfirmDeleteSubcategory -> handleDeleteSubcategory(event.subcategoryId)
        CategoriesEvent.DismissDeleteSubcategoryDialog -> {
            flowOf(CategoriesResult.HideDeleteSubcategoryDialog)
        }

        CategoriesEvent.DismissError -> flowOf(CategoriesResult.ClearError)
    }

    @Suppress("CyclomaticComplexMethod")
    override fun getStateByResult(
        previous: CategoriesState,
        result: DomainResult
    ): CategoriesState {
        return when (result) {
            is ObserveCategoriesUseCase.Result.Success -> {
                val uiModels = result.data.toUiModels()
                previous.withCategories(uiModels)
            }

            is ObserveCategoriesUseCase.Result.Failure -> previous.toError(result.errorType)
            is CategoriesResult.ShowAddCategorySheet -> previous.updateSheet(showAddCategory = true)
            is CategoriesResult.HideAddCategorySheet -> previous.updateSheet(showAddCategory = false)
            is CategoriesResult.ShowAddSubcategorySheet -> {
                previous.updateSubcategorySheet(result.categoryId)
            }

            is CategoriesResult.HideAddSubcategorySheet -> previous.updateSubcategorySheet(null)
            is CategoriesResult.ShowEditCategorySheet -> previous.updateEditingCategory(result.category)
            is CategoriesResult.HideEditCategorySheet -> previous.updateEditingCategory(null)
            is CategoriesResult.HideDeletionDialog -> previous.withDeletionDialog(null)
            is CategoriesResult.ShowSuccessMessage -> previous.withSuccessMessage(result.message)

            is AddCategoryUseCase.Result.Success -> previous.updateSheet(showAddCategory = false)
            is AddSubcategoryUseCase.Result.Success -> previous.updateSubcategorySheet(null)
            is UpdateCategoryUseCase.Result.Success -> previous.updateEditingCategory(null)
            is DeleteCategoryUseCase.Result.HasExpenses -> {
                val targets = previous.categories
                    .filter { it.id != result.categoryId }
                    .map { CategoryTargetUiModel(it.id, it.name, it.iconKey) }
                    .toImmutableList()
                previous.withDeletionDialog(
                    CategoryDeletionDialog.WithExpenses(result.categoryId, result.count, targets),
                )
            }

            is DeleteCategoryUseCase.Result.NoExpenses -> {
                previous.withDeletionDialog(CategoryDeletionDialog.SimpleConfirm(result.categoryId))
            }

            is DeleteCategoryUseCase.Result.Success -> previous.withDeletionDialog(null)
            is CategoriesResult.ShowDeleteSubcategoryDialog -> {
                previous.withSubcategoryToDelete(result.subcategory)
            }
            is CategoriesResult.HideDeleteSubcategoryDialog -> {
                previous.withSubcategoryToDelete(null)
            }
            is DeleteSubcategoryUseCase.Result.Success -> previous.withSubcategoryToDelete(null)
            else -> super.getStateByResult(previous, result)
        }
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType
    ): CategoriesState {
        return when (result) {
            is DeleteSubcategoryUseCase.Result.Failure -> {
                state.value.withSubcategoryToDelete(null).toError(result.errorType)
            }

            is DeleteCategoryUseCase.Result.Failure -> {
                state.value.withDeletionDialog(null).toError(result.errorType)
            }

            is UpdateCategoryUseCase.Result.Failure -> {
                state.value.updateEditingCategory(null).toError(result.errorType)
            }

            is AddSubcategoryUseCase.Result.Failure -> {
                state.value.updateSubcategorySheet(null).toError(result.errorType)
            }

            is CategoriesResult.ClearError -> state.value.toContent().withSuccessMessage(null)

            else -> state.value.toError(errorType)
        }
    }

    private fun handleAddCategory(event: CategoriesEvent.ConfirmAddCategory): Flow<DomainResult> =
        flow {
            val trimmedComment = event.comment?.trim()?.ifEmpty { null }
            val name = event.name.trim()
            val result = addCategoryUseCase(name, event.iconKey, trimmedComment)
            emit(result)
            if (result is AddCategoryUseCase.Result.Success) {
                emit(CategoriesResult.ShowSuccessMessage("Category $name created"))
            }
        }

    private fun handleAddSubcategory(
        event: CategoriesEvent.ConfirmAddSubcategory,
    ): Flow<DomainResult> = flow {
        val trimmedComment = event.comment?.trim()?.ifEmpty { null }
        emit(addSubcategoryUseCase(event.categoryId, event.name.trim(), trimmedComment))
    }

    private fun handleEditCategory(event: CategoriesEvent.ConfirmEditCategory): Flow<DomainResult> =
        flow {
            val trimmedComment = event.comment?.trim()?.ifEmpty { null }
            emit(updateCategoryUseCase(event.id, event.name.trim(), event.iconKey, trimmedComment))
        }

    private fun handleCheckDeleteCategory(categoryId: Long): Flow<DomainResult> = flow {
        emit(deleteCategoryUseCase.checkExpenses(categoryId))
    }

    private fun handleDeleteEmpty(categoryId: Long): Flow<DomainResult> = flow {
        val name = state.value.categories.find { it.id == categoryId }?.name
        val result = deleteCategoryUseCase.deleteEmpty(categoryId)
        emit(result)
        if (result is DeleteCategoryUseCase.Result.Success && name != null) {
            emit(CategoriesResult.ShowSuccessMessage("Category $name deleted"))
        }
    }

    private fun handleDeleteWithAction(
        categoryId: Long,
        action: ExpenseDeletionAction,
    ): Flow<DomainResult> = flow {
        val categories = state.value.categories
        val name = categories.find { it.id == categoryId }?.name
        val result = deleteCategoryUseCase(categoryId, action)
        emit(result)
        if (result is DeleteCategoryUseCase.Result.Success && name != null) {
            val message = when (action) {
                is ExpenseDeletionAction.MoveToCategory -> {
                    val targetName = categories.find { it.id == action.targetCategoryId }?.name
                    "Category $name deleted. Expenses moved to $targetName"
                }

                ExpenseDeletionAction.DeleteExpenses -> "Category $name deleted"
            }
            emit(CategoriesResult.ShowSuccessMessage(message))
        }
    }

    private fun handleDeleteSubcategory(subcategoryId: Long): Flow<DomainResult> = flow {
        val name = state.value.subcategoryToDelete?.name
        val result = deleteSubcategoryUseCase(subcategoryId)
        emit(result)
        if (result is DeleteSubcategoryUseCase.Result.Success && name != null) {
            emit(CategoriesResult.ShowSuccessMessage("Subcategory $name deleted"))
        }
    }

    private fun CategoriesState.withCategories(
        newCategories: ImmutableList<CategoryRowUiModel>,
    ): CategoriesState = when (this) {
        CategoriesState.Loading -> CategoriesState.Content(
            categories = newCategories,
            showAddCategorySheet = false,
            addSubcategoryForCategoryId = null,
            editingCategory = null,
        )

        is CategoriesState.Content -> copy(categories = newCategories)
        is CategoriesState.Error -> CategoriesState.Content(
            categories = newCategories,
            showAddCategorySheet = showAddCategorySheet,
            addSubcategoryForCategoryId = addSubcategoryForCategoryId,
            editingCategory = editingCategory,
            deletionDialog = deletionDialog,
            subcategoryToDelete = subcategoryToDelete,
        )
    }

    private fun CategoriesState.toError(errorType: ErrorType): CategoriesState.Error =
        CategoriesState.Error(
            errorType = errorType,
            categories = categories,
            showAddCategorySheet = showAddCategorySheet,
            addSubcategoryForCategoryId = addSubcategoryForCategoryId,
            editingCategory = editingCategory,
            deletionDialog = deletionDialog,
            subcategoryToDelete = subcategoryToDelete,
        )

    private fun CategoriesState.toContent(): CategoriesState = when (this) {
        is CategoriesState.Error -> CategoriesState.Content(
            categories = categories,
            showAddCategorySheet = showAddCategorySheet,
            addSubcategoryForCategoryId = addSubcategoryForCategoryId,
            editingCategory = editingCategory,
            deletionDialog = deletionDialog,
            subcategoryToDelete = subcategoryToDelete,
        )

        else -> this
    }

    private fun CategoriesState.updateSheet(showAddCategory: Boolean): CategoriesState =
        when (this) {
            is CategoriesState.Content -> copy(showAddCategorySheet = showAddCategory)
            is CategoriesState.Error -> copy(showAddCategorySheet = showAddCategory)
            CategoriesState.Loading -> this
        }

    private fun CategoriesState.updateSubcategorySheet(categoryId: Long?): CategoriesState =
        when (this) {
            is CategoriesState.Content -> copy(addSubcategoryForCategoryId = categoryId)
            is CategoriesState.Error -> copy(addSubcategoryForCategoryId = categoryId)
            CategoriesState.Loading -> this
        }

    private fun CategoriesState.updateEditingCategory(
        category: CategoryRowUiModel?,
    ): CategoriesState = when (this) {
        is CategoriesState.Content -> copy(editingCategory = category)
        is CategoriesState.Error -> copy(editingCategory = category)
        CategoriesState.Loading -> this
    }

    private fun CategoriesState.withDeletionDialog(
        dialog: CategoryDeletionDialog?,
    ): CategoriesState = when (this) {
        is CategoriesState.Content -> copy(deletionDialog = dialog)
        is CategoriesState.Error -> copy(deletionDialog = dialog)
        CategoriesState.Loading -> this
    }

    private fun CategoriesState.withSubcategoryToDelete(
        subcategory: SubcategoryChipUiModel?,
    ): CategoriesState = when (this) {
        is CategoriesState.Content -> copy(subcategoryToDelete = subcategory)
        is CategoriesState.Error -> copy(subcategoryToDelete = subcategory)
        CategoriesState.Loading -> this
    }

    private fun CategoriesState.withSuccessMessage(message: String?): CategoriesState =
        when (this) {
            is CategoriesState.Content -> copy(successMessage = message)
            is CategoriesState.Error -> copy(successMessage = message)
            CategoriesState.Loading -> this
        }
}

private fun List<CategoryWithSubcategories>.toUiModels(): ImmutableList<CategoryRowUiModel> =
    map { item ->
        CategoryRowUiModel(
            id = item.category.id,
            name = item.category.name,
            iconKey = item.category.iconKey,
            comment = item.category.comment,
            subcategories = item.subcategories?.map { sub ->
                SubcategoryChipUiModel(id = sub.id, name = sub.name, comment = sub.comment)
            }?.toImmutableList(),
        )
    }.toImmutableList()

private sealed interface CategoriesResult : DomainResult {
    data object ShowAddCategorySheet : CategoriesResult
    data object HideAddCategorySheet : CategoriesResult
    data class ShowAddSubcategorySheet(val categoryId: Long) : CategoriesResult
    data object HideAddSubcategorySheet : CategoriesResult
    data class ShowEditCategorySheet(val category: CategoryRowUiModel) : CategoriesResult
    data object HideEditCategorySheet : CategoriesResult
    data object HideDeletionDialog : CategoriesResult
    data class ShowDeleteSubcategoryDialog(val subcategory: SubcategoryChipUiModel) :
        CategoriesResult

    data object HideDeleteSubcategoryDialog : CategoriesResult
    data class ShowSuccessMessage(val message: String) : CategoriesResult
    data object ClearError : CategoriesResult
}
