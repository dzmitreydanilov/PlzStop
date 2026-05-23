package com.please.stop.app.features.categories.presentation.archived

import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import com.please.stop.app.features.categories.presentation.CategoryRowUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
sealed interface ArchivedCategoriesState {

    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<CategoryRowUiModel>

    @Serializable
    data object Loading : ArchivedCategoriesState {
        override val categories: ImmutableList<CategoryRowUiModel> = persistentListOf()
    }

    @Serializable
    data class Content(
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryRowUiModel>,
    ) : ArchivedCategoriesState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<CategoryRowUiModel>,
    ) : ArchivedCategoriesState
}
