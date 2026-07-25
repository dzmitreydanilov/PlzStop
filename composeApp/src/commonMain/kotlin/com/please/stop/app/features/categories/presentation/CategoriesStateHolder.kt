package com.please.stop.app.features.categories.presentation

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.UiEffect
import com.please.stop.app.features.categories.domain.model.CategorySummary
import com.please.stop.app.features.categories.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.AddSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.ArchiveCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.DeleteSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.LoadSubcategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.ObserveCategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.UpdateCategoryUseCase
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass
import com.please.stop.app.core.models.domain.Result as DomainResult

class CategoriesStateHolder(
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val addSubcategoryUseCase: AddSubcategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val archiveCategoryUseCase: ArchiveCategoryUseCase,
    private val deleteSubcategoryUseCase: DeleteSubcategoryUseCase,
    private val loadSubcategoriesUseCase: LoadSubcategoriesUseCase,
) : StateHolder<CategoriesState, CategoriesEvent>() {

    override val tag = "CategoriesStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): CategoriesState = CategoriesState.Loading

    override fun getEffectResults(): Set<KClass<out DomainResult>> = setOf(
        CategoriesResult.ShowSuccessMessage::class,
    )

    override fun getEffectByResult(result: DomainResult): UiEffect? = when (result) {
        is CategoriesResult.ShowSuccessMessage -> UiEffect.ShowMessage(result.message)
        else -> null
    }

    override fun collectWhileSubscribed(): Flow<DomainResult> = observeCategoriesUseCase()

    @Suppress("CyclomaticComplexMethod")
    override fun resolveEventResult(event: CategoriesEvent): Flow<DomainResult> = when (event) {
        CategoriesEvent.AddCategoryClicked -> flowOf(CategoriesResult.ShowAddCategorySheet)
        is CategoriesEvent.ConfirmAddCategory -> handleAddCategory(event)
        CategoriesEvent.DismissAddCategorySheet -> flowOf(CategoriesResult.HideAddCategorySheet)
        is CategoriesEvent.ExpandSubcategories -> loadSubcategories(event.categoryId)
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
        is CategoriesEvent.ArchiveCategoryClicked -> handleShowArchiveDialog(event.categoryId)
        is CategoriesEvent.ConfirmArchiveCategory -> handleArchiveCategory(event.categoryId)
        CategoriesEvent.DismissArchiveDialog -> flowOf(CategoriesResult.HideArchiveDialog)
        is CategoriesEvent.DeleteSubcategoryClicked -> {
            flowOf(CategoriesResult.ShowDeleteSubcategoryDialog(event.subcategory))
        }

        is CategoriesEvent.ConfirmDeleteSubcategory -> {
            handleDeleteSubcategory(event.subcategoryId)
        }

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
                val uiModels = result.data.toUiModels(previous.categories)
                previous.withCategories(uiModels)
            }

            is ObserveCategoriesUseCase.Result.Failure -> previous.toError(result.errorType)
            is LoadSubcategoriesUseCase.Result.Success -> {
                previous.withSubcategories(result.categoryId, result.subcategories.toUiModels())
            }
            is CategoriesResult.ShowAddCategorySheet -> previous.updateSheet(showAddCategory = true)
            is CategoriesResult.HideAddCategorySheet -> previous.updateSheet(showAddCategory = false)
            is CategoriesResult.ShowAddSubcategorySheet -> {
                previous.updateSubcategorySheet(result.categoryId)
            }

            is CategoriesResult.HideAddSubcategorySheet -> previous.updateSubcategorySheet(null)
            is CategoriesResult.ShowEditCategorySheet -> previous.updateEditingCategory(result.category)
            is CategoriesResult.HideEditCategorySheet -> previous.updateEditingCategory(null)
            is CategoriesResult.HideArchiveDialog -> previous.withArchiveDialog(null)
            is CategoriesResult.ShowArchiveDialog -> {
                previous.withArchiveDialog(
                    CategoryArchiveDialog(result.categoryId, result.categoryName),
                )
            }

            is AddCategoryUseCase.Result.Success -> previous.updateSheet(showAddCategory = false)
            is AddSubcategoryUseCase.Result.Success -> previous.updateSubcategorySheet(null)
            is UpdateCategoryUseCase.Result.Success -> previous.updateEditingCategory(null)
            is ArchiveCategoryUseCase.Result.Success -> previous.withArchiveDialog(null)
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

            is ArchiveCategoryUseCase.Result.Failure -> {
                state.value.withArchiveDialog(null).toError(result.errorType)
            }

            is UpdateCategoryUseCase.Result.Failure -> {
                state.value.updateEditingCategory(null).toError(result.errorType)
            }

            is AddSubcategoryUseCase.Result.Failure -> {
                state.value.updateSubcategorySheet(null).toError(result.errorType)
            }

            is CategoriesResult.ClearError -> state.value.toContent()

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

    private fun loadSubcategories(categoryId: Long): Flow<DomainResult> = flow {
        emit(loadSubcategoriesUseCase(categoryId))
    }

    private fun handleAddSubcategory(
        event: CategoriesEvent.ConfirmAddSubcategory,
    ): Flow<DomainResult> = flow {
        val trimmedComment = event.comment?.trim()?.ifEmpty { null }
        val result = addSubcategoryUseCase(event.categoryId, event.name.trim(), trimmedComment)
        emit(result)
        if (result is AddSubcategoryUseCase.Result.Success) {
            emit(loadSubcategoriesUseCase(event.categoryId))
        }
    }

    private fun handleEditCategory(event: CategoriesEvent.ConfirmEditCategory): Flow<DomainResult> =
        flow {
            val trimmedComment = event.comment?.trim()?.ifEmpty { null }
            emit(updateCategoryUseCase(event.id, event.name.trim(), event.iconKey, trimmedComment))
        }

    private fun handleShowArchiveDialog(categoryId: Long): Flow<DomainResult> = flow {
        val name = state.value.categories.find { it.id == categoryId }?.name.orEmpty()
        emit(CategoriesResult.ShowArchiveDialog(categoryId, name))
    }

    private fun handleArchiveCategory(categoryId: Long): Flow<DomainResult> = flow {
        val name = state.value.categories.find { it.id == categoryId }?.name
        val result = archiveCategoryUseCase(categoryId)
        emit(result)
        if (result is ArchiveCategoryUseCase.Result.Success && name != null) {
            emit(CategoriesResult.ShowSuccessMessage("Category $name archived"))
        }
    }

    private fun handleDeleteSubcategory(subcategoryId: Long): Flow<DomainResult> = flow {
        val name = state.value.subcategoryToDelete?.name
        val categoryId = state.value.categories.firstOrNull { category ->
            category.subcategories?.any { it.id == subcategoryId } == true
        }?.id
        val result = deleteSubcategoryUseCase(subcategoryId)
        emit(result)
        if (result is DeleteSubcategoryUseCase.Result.Success && categoryId != null) {
            emit(loadSubcategoriesUseCase(categoryId))
        }
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
            archiveDialog = archiveDialog,
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
            archiveDialog = archiveDialog,
            subcategoryToDelete = subcategoryToDelete,
        )

    private fun CategoriesState.toContent(): CategoriesState = when (this) {
        is CategoriesState.Error -> CategoriesState.Content(
            categories = categories,
            showAddCategorySheet = showAddCategorySheet,
            addSubcategoryForCategoryId = addSubcategoryForCategoryId,
            editingCategory = editingCategory,
            archiveDialog = archiveDialog,
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

    private fun CategoriesState.withArchiveDialog(
        dialog: CategoryArchiveDialog?,
    ): CategoriesState = when (this) {
        is CategoriesState.Content -> copy(archiveDialog = dialog)
        is CategoriesState.Error -> copy(archiveDialog = dialog)
        CategoriesState.Loading -> this
    }

    private fun CategoriesState.withSubcategoryToDelete(
        subcategory: SubcategoryChipUiModel?,
    ): CategoriesState = when (this) {
        is CategoriesState.Content -> copy(subcategoryToDelete = subcategory)
        is CategoriesState.Error -> copy(subcategoryToDelete = subcategory)
        CategoriesState.Loading -> this
    }

    private fun CategoriesState.withSubcategories(
        categoryId: Long,
        subcategories: ImmutableList<SubcategoryChipUiModel>,
    ): CategoriesState {
        val updated = categories.map { category ->
            if (category.id == categoryId) category.copy(subcategories = subcategories) else category
        }.toImmutableList()
        return withCategories(updated)
    }
}

private fun List<CategorySummary>.toUiModels(
    previousCategories: ImmutableList<CategoryRowUiModel>,
): ImmutableList<CategoryRowUiModel> =
    map { item ->
        val previousSubcategories = previousCategories
            .firstOrNull { it.id == item.category.id }
            ?.subcategories
        CategoryRowUiModel(
            id = item.category.id,
            name = item.category.name,
            iconKey = item.category.iconKey,
            comment = item.category.comment,
            subcategoryCount = item.subcategoryCount,
            subcategories = if (item.subcategoryCount == null) {
                null
            } else {
                previousSubcategories ?: persistentListOf()
            },
        )
    }.toImmutableList()

private fun ImmutableList<Subcategory>.toUiModels(): ImmutableList<SubcategoryChipUiModel> =
    map { subcategory ->
        SubcategoryChipUiModel(
            id = subcategory.id,
            name = subcategory.name,
            comment = subcategory.comment,
        )
    }.toImmutableList()

private sealed interface CategoriesResult : DomainResult {
    data object ShowAddCategorySheet : CategoriesResult
    data object HideAddCategorySheet : CategoriesResult
    data class ShowAddSubcategorySheet(val categoryId: Long) : CategoriesResult
    data object HideAddSubcategorySheet : CategoriesResult
    data class ShowEditCategorySheet(val category: CategoryRowUiModel) : CategoriesResult
    data object HideEditCategorySheet : CategoriesResult
    data class ShowArchiveDialog(val categoryId: Long, val categoryName: String) : CategoriesResult
    data object HideArchiveDialog : CategoriesResult
    data class ShowDeleteSubcategoryDialog(val subcategory: SubcategoryChipUiModel) :
        CategoriesResult

    data object HideDeleteSubcategoryDialog : CategoriesResult
    data class ShowSuccessMessage(val message: String) : CategoriesResult
    data object ClearError : CategoriesResult
}
