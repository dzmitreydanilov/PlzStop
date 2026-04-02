package com.please.stop.app.features.home.presentation

import androidx.compose.runtime.Stable
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.serialization.ImmutableListSerializer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Stable
@Serializable
sealed interface HomeState {

    @Serializable
    data object Loading : HomeState

    @Serializable
    data class Content(
        val displayName: String? = null,
        val currencySymbol: String = "$",
        val decimalPlaces: Int = 2,
        val totalSpentFormatted: String = "",
        @Serializable(with = ImmutableListSerializer::class)
        val categories: ImmutableList<HomeCategoryUiModel> = persistentListOf(),
        val hasAnyExpenses: Boolean = false,
        val showAddCategorySheet: Boolean = false,
        val errorType: ErrorType? = null,
    ) : HomeState

    @Serializable
    data class Error(val errorType: ErrorType) : HomeState
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
