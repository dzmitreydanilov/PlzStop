package com.please.stop.app.features.analytics.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList

@Stable
sealed interface AnalyticsState {

    val totalSpentFormatted: String?
    val categoriesCount: Int
    val activeCategoriesCount: Int
    val hasAnyExpenses: Boolean

    data object Loading : AnalyticsState {
        override val totalSpentFormatted: String? = null
        override val categoriesCount: Int = 0
        override val activeCategoriesCount: Int = 0
        override val hasAnyExpenses: Boolean = false
    }

    data class Content(
        override val totalSpentFormatted: String?,
        override val categoriesCount: Int,
        override val activeCategoriesCount: Int,
        override val hasAnyExpenses: Boolean,
        val spendingSlices: ImmutableList<SpendingSlice>,
    ) : AnalyticsState

    data class Error(
        override val totalSpentFormatted: String?,
        override val categoriesCount: Int,
        override val activeCategoriesCount: Int,
        override val hasAnyExpenses: Boolean,
    ) : AnalyticsState
}

@Stable
data class SpendingSlice(
    val name: String,
    val formattedAmount: String,
    val amount: Float,
    val color: Color,
)
