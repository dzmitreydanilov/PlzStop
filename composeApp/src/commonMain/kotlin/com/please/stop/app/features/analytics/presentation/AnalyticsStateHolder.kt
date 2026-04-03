package com.please.stop.app.features.analytics.presentation

import androidx.compose.ui.graphics.Color
import com.please.stop.app.core.BootstrapTiming
import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.features.home.domain.model.HomeData
import com.please.stop.app.features.home.domain.usecase.ObserveHomeDataUseCase
import com.please.stop.app.utils.formatCurrencyAmount
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow

class AnalyticsStateHolder(
    private val observeHomeDataUseCase: ObserveHomeDataUseCase,
) : StateHolder<AnalyticsState, Nothing>() {

    override val tag = "AnalyticsStateHolder"
    override val bootstrapTiming = BootstrapTiming.DEFERRED

    override fun getInitial(): AnalyticsState = AnalyticsState.Loading

    override fun collectFlowsOnInit(): Flow<Result> = observeHomeDataUseCase()

    override fun getStateByResult(previous: AnalyticsState, result: Result): AnalyticsState {
        return when (result) {
            is ObserveHomeDataUseCase.Result.Success -> result.data.toContent()
            is ObserveHomeDataUseCase.Result.Failure -> previous.toError()
            else -> super.getStateByResult(previous, result)
        }
    }

    override fun getErrorStateByResult(result: Result, errorType: ErrorType): AnalyticsState {
        return state.value.toError()
    }

    private fun HomeData.toContent(): AnalyticsState.Content {
        val spendingCategories = categories.filter { it.spentMinorUnits > 0 }

        return AnalyticsState.Content(
            totalSpentFormatted = formatCurrencyAmount(
                totalSpentMinorUnits,
                currency.symbol,
                decimalPlaces,
            ),
            categoriesCount = categories.size,
            activeCategoriesCount = spendingCategories.size,
            hasAnyExpenses = totalSpentMinorUnits > 0,
            spendingSlices = spendingCategories.mapIndexed { i, item ->
                SpendingSlice(
                    name = item.name,
                    formattedAmount = formatCurrencyAmount(
                        item.spentMinorUnits,
                        currency.symbol,
                        decimalPlaces,
                    ),
                    amount = item.spentMinorUnits.toFloat(),
                    color = chartColor(i),
                )
            }.toImmutableList(),
        )
    }

    private fun AnalyticsState.toError(): AnalyticsState.Error = AnalyticsState.Error(
        totalSpentFormatted = totalSpentFormatted,
        categoriesCount = categoriesCount,
        activeCategoriesCount = activeCategoriesCount,
        hasAnyExpenses = hasAnyExpenses,
    )

    companion object {
        private val CHART_PALETTE = listOf(
            Color(0xFF14B8A6), // teal
            Color(0xFF3B82F6), // blue
            Color(0xFF8B5CF6), // purple
            Color(0xFF10B981), // emerald
            Color(0xFFF59E0B), // amber
            Color(0xFFEC4899), // pink
        )

        private fun chartColor(index: Int): Color =
            CHART_PALETTE[index % CHART_PALETTE.size]
    }
}
