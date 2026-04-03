package com.please.stop.app.features.home.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.Currency
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface HomeState {
    val displayName: String?
    val currency: Currency?
    val totalSpentFormatted: String?

    @Serializable(with = ImmutableListSerializer::class)
    val categories: ImmutableList<HomeCategoryUiModel>
    val hasAnyExpenses: Boolean
    val showAddCategorySheet: Boolean

    @Serializable
    data object Loading : HomeState {
        override val displayName: String? = null
        override val currency: Currency? = null
        override val totalSpentFormatted: String? = null
        override val categories: ImmutableList<HomeCategoryUiModel> = persistentListOf()
        override val hasAnyExpenses: Boolean = false
        override val showAddCategorySheet: Boolean = false
    }

    @Serializable
    data class Content(
        override val displayName: String?,
        override val currency: Currency?,
        override val totalSpentFormatted: String?,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<HomeCategoryUiModel>,
        override val hasAnyExpenses: Boolean,
        override val showAddCategorySheet: Boolean,
    ) : HomeState

    @Serializable
    data class Error(
        val errorType: ErrorType,
        override val displayName: String?,
        override val currency: Currency?,
        override val totalSpentFormatted: String?,
        @Serializable(with = ImmutableListSerializer::class)
        override val categories: ImmutableList<HomeCategoryUiModel>,
        override val hasAnyExpenses: Boolean,
        override val showAddCategorySheet: Boolean,
    ) : HomeState
}

@Stable
@Serializable
data class HomeCategoryUiModel(
    val id: Long,
    val name: String,
    val iconKey: String,
    val spentFormatted: String,
    val hasSpending: Boolean,
)
