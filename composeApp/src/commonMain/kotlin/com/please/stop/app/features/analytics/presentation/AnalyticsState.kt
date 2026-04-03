package com.please.stop.app.features.analytics.presentation

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.himanshoe.charty.bar.model.BarData
import com.himanshoe.charty.pie.model.PieChartData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
        val pieChartData: ImmutableList<PieChartData>,
        val barChartData: ImmutableList<BarData>,
        val legend: ImmutableList<LegendItem>,
    ) : AnalyticsState

    data class Error(
        override val totalSpentFormatted: String?,
        override val categoriesCount: Int,
        override val activeCategoriesCount: Int,
        override val hasAnyExpenses: Boolean,
    ) : AnalyticsState
}

@Stable
data class LegendItem(
    val color: Color,
    val name: String,
    val value: String,
)
