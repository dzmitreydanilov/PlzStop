package com.please.stop.app.features.categories.presentation.archived

import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.presentation.UiEffect
import com.please.stop.app.features.categories.domain.model.CategoryWithSubcategories
import com.please.stop.app.features.categories.domain.usecase.ObserveArchivedCategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.UnarchiveCategoryUseCase
import com.please.stop.app.features.categories.presentation.CategoryRowUiModel
import com.please.stop.app.features.categories.presentation.SubcategoryChipUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass
import com.please.stop.app.core.models.domain.Result as DomainResult

class ArchivedCategoriesStateHolder(
    private val observeArchivedCategoriesUseCase: ObserveArchivedCategoriesUseCase,
    private val unarchiveCategoryUseCase: UnarchiveCategoryUseCase,
) : StateHolder<ArchivedCategoriesState, ArchivedCategoriesEvent>() {

    override val tag = "ArchivedCategoriesStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): ArchivedCategoriesState = ArchivedCategoriesState.Loading

    override fun getEffectResults(): Set<KClass<out DomainResult>> = setOf(
        ArchivedResult.ShowSuccessMessage::class,
    )

    override fun getEffectByResult(result: DomainResult): UiEffect? = when (result) {
        is ArchivedResult.ShowSuccessMessage -> UiEffect.ShowMessage(result.message)
        else -> null
    }

    override fun collectWhileSubscribed(): Flow<DomainResult> = observeArchivedCategoriesUseCase()

    override fun resolveEventResult(
        event: ArchivedCategoriesEvent,
    ): Flow<DomainResult> = when (event) {
        is ArchivedCategoriesEvent.RestoreCategoryClicked -> handleRestore(event.categoryId)
        ArchivedCategoriesEvent.DismissError -> flowOf(ArchivedResult.ClearError)
    }

    override fun getStateByResult(
        previous: ArchivedCategoriesState,
        result: DomainResult,
    ): ArchivedCategoriesState = when (result) {
        is ObserveArchivedCategoriesUseCase.Result.Success -> {
            previous.withCategories(result.data.toUiModels())
        }
        is ObserveArchivedCategoriesUseCase.Result.Failure -> previous.toError(result.errorType)
        is UnarchiveCategoryUseCase.Result.Success -> previous
        else -> super.getStateByResult(previous, result)
    }

    override fun getErrorStateByResult(
        result: DomainResult,
        errorType: ErrorType,
    ): ArchivedCategoriesState = when (result) {
        is ArchivedResult.ClearError -> state.value.toContent()
        else -> state.value.toError(errorType)
    }

    private fun handleRestore(categoryId: Long): Flow<DomainResult> = flow {
        val name = state.value.categories.find { it.id == categoryId }?.name
        val result = unarchiveCategoryUseCase(categoryId)
        emit(result)
        if (result is UnarchiveCategoryUseCase.Result.Success && name != null) {
            emit(ArchivedResult.ShowSuccessMessage("Category $name restored"))
        }
    }

    private fun ArchivedCategoriesState.withCategories(
        newCategories: ImmutableList<CategoryRowUiModel>,
    ): ArchivedCategoriesState = when (this) {
        ArchivedCategoriesState.Loading -> ArchivedCategoriesState.Content(
            categories = newCategories,
        )
        is ArchivedCategoriesState.Content -> copy(categories = newCategories)
        is ArchivedCategoriesState.Error -> ArchivedCategoriesState.Content(
            categories = newCategories,
        )
    }

    private fun ArchivedCategoriesState.toError(
        errorType: ErrorType,
    ): ArchivedCategoriesState.Error = ArchivedCategoriesState.Error(
        errorType = errorType,
        categories = categories,
    )

    private fun ArchivedCategoriesState.toContent(): ArchivedCategoriesState = when (this) {
        is ArchivedCategoriesState.Error -> ArchivedCategoriesState.Content(
            categories = categories,
        )
        else -> this
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

private sealed interface ArchivedResult : DomainResult {
    data class ShowSuccessMessage(val message: String) : ArchivedResult
    data object ClearError : ArchivedResult
}
