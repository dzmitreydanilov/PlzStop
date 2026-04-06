package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.please.stop.app.features.analytics.presentation.AnalyticsEvent
import com.please.stop.app.features.analytics.presentation.AnalyticsState
import com.please.stop.app.features.analytics.presentation.AnalyticsStateHolder
import com.please.stop.app.features.analytics.presentation.BudgetBurnUi
import com.please.stop.app.features.analytics.presentation.CategoryProgressUi
import com.please.stop.app.features.analytics.presentation.DailySpendingUiPoint
import com.please.stop.app.features.analytics.presentation.MonthlyBarUiItem
import com.please.stop.app.features.analytics.presentation.ProjectedTotalUi
import com.please.stop.app.features.analytics.presentation.SpendingSlice
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.animation.FadeSlideIn
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_avg_daily
import plzstop.composeapp.generated.resources.analytics_budget_pulse
import plzstop.composeapp.generated.resources.analytics_budget_spent_of
import plzstop.composeapp.generated.resources.analytics_category_breakdown
import plzstop.composeapp.generated.resources.analytics_daily_allowance
import plzstop.composeapp.generated.resources.analytics_daily_trends
import plzstop.composeapp.generated.resources.analytics_empty_hint
import plzstop.composeapp.generated.resources.analytics_empty_title
import plzstop.composeapp.generated.resources.analytics_insights_subtitle
import plzstop.composeapp.generated.resources.analytics_loading
import plzstop.composeapp.generated.resources.analytics_monthly_comparison
import plzstop.composeapp.generated.resources.analytics_projected_on_track
import plzstop.composeapp.generated.resources.analytics_projected_over
import plzstop.composeapp.generated.resources.analytics_projected_under
import plzstop.composeapp.generated.resources.analytics_spending_overview
import plzstop.composeapp.generated.resources.analytics_stat_active
import plzstop.composeapp.generated.resources.analytics_stat_categories
import plzstop.composeapp.generated.resources.analytics_stat_total_spent
import plzstop.composeapp.generated.resources.analytics_tab

private const val LEGEND_DOT_CORNER_PERCENT = 50
private const val CHART_HEIGHT = 220
private const val PROGRESS_BAR_HEIGHT = 8
private const val BUDGET_BAR_HEIGHT = 14
private const val BURN_THRESHOLD_GREEN = 0.6f
private const val BURN_THRESHOLD_YELLOW = 0.85f
private val GhostLineThickness = 1.5.dp

@Composable
fun AnalyticsScreen() {
    val stateHolder = koinViewModel<AnalyticsStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    val appColors = LocalAppColors.current
    val pieModelProducer = remember { PieChartModelProducer() }
    val monthlyModelProducer = remember { CartesianChartModelProducer() }
    val dailyModelProducer = remember { CartesianChartModelProducer() }

    val contentState = state as? AnalyticsState.Content
    val sliceAmounts = contentState?.spendingSlices?.map { it.amount }
    LaunchedEffect(sliceAmounts) {
        sliceAmounts ?: return@LaunchedEffect
        pieModelProducer.runTransaction {
            pieSeries { series(sliceAmounts) }
        }
    }
    val barAmounts = contentState?.monthlyBars?.map { it.amount }
    LaunchedEffect(barAmounts) {
        if (barAmounts.isNullOrEmpty()) return@LaunchedEffect
        monthlyModelProducer.runTransaction {
            columnSeries { series(barAmounts) }
        }
    }
    val dailyAmounts = contentState?.dailySpending?.map { it.amount }
    val pacingPoints = contentState?.budgetPacingPoints
    LaunchedEffect(dailyAmounts, pacingPoints) {
        if (dailyAmounts.isNullOrEmpty()) return@LaunchedEffect
        dailyModelProducer.runTransaction {
            lineSeries {
                series(dailyAmounts)
                if (!pacingPoints.isNullOrEmpty()) {
                    series(pacingPoints)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FadeSlideIn {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(appColors.headerGradient)
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.analytics_tab),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.analytics_insights_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is AnalyticsState.Content -> DashboardContent(
                state = s,
                onEvent = stateHolder::processEvent,
                pieModelProducer = pieModelProducer,
                monthlyModelProducer = monthlyModelProducer,
                dailyModelProducer = dailyModelProducer,
            )
            else -> LoadingPlaceholder()
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.analytics_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DashboardContent(
    state: AnalyticsState.Content,
    onEvent: (AnalyticsEvent) -> Unit,
    pieModelProducer: PieChartModelProducer,
    monthlyModelProducer: CartesianChartModelProducer,
    dailyModelProducer: CartesianChartModelProducer,
) {
    val shimmerOffset = rememberShimmerOffset()
    val appColors = LocalAppColors.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FadeSlideIn(delayMillis = 100) {
            QuickStatRow(state)
        }

        if (state.hasAnyExpenses && state.spendingSlices.isNotEmpty()) {
            // 1. Budget Pulse
            state.budgetBurn?.let { burn ->
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 150) {
                    BudgetPulseCard(burn)
                }
            }

            // 2. Donut Chart — Spending Overview (Smart Legend)
            // Vico pie chart crashes/disappears with a single slice; skip donut in that case
            if (state.spendingSlices.size > 1) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 200) {
                    SpendingOverviewCard(
                        slices = state.spendingSlices,
                        pieModelProducer = pieModelProducer,
                        shimmerOffset = shimmerOffset,
                    )
                }
            }

            // 3. Line Chart — Daily Trends (Ghost Line)
            if (state.dailySpending.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 300) {
                    DailyTrendsCard(
                        dailySpending = state.dailySpending,
                        hasBudgetPacing = state.budgetPacingPoints.isNotEmpty(),
                        dailyModelProducer = dailyModelProducer,
                    )
                }
            }

            // 4. Heatmap
            if (state.heatmapDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 400) {
                    SpendingHeatmapCard(
                        heatmapDays = state.heatmapDays,
                        accentColor = appColors.chartColors.first(),
                        onDayTap = { day -> onEvent(AnalyticsEvent.DayTapped(day)) },
                    )
                }
            }

            // 5. Monthly Comparison (with Projected Total)
            if (state.monthlyBars.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 500) {
                    MonthlyComparisonCard(
                        monthlyBars = state.monthlyBars,
                        projectedTotal = state.projectedTotal,
                        monthlyModelProducer = monthlyModelProducer,
                    )
                }
            }

            // 6. Category Breakdown
            if (state.categoryProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 600) {
                    CategoryProgressCard(categoryProgress = state.categoryProgress)
                }
            }
        }

        if (!state.hasAnyExpenses) {
            Spacer(modifier = Modifier.height(20.dp))
            FadeSlideIn(delayMillis = 200) {
                EmptyStateCard()
            }
        }
    }

    if (state.selectedDaySheet != null || state.isDaySheetLoading) {
        DayExpensesSheet(
            sheetUi = state.selectedDaySheet,
            isLoading = state.isDaySheetLoading,
            onDismiss = { onEvent(AnalyticsEvent.DismissDaySheet) },
        )
    }
}

@Composable
private fun QuickStatRow(state: AnalyticsState.Content) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickStatCard(
            emoji = "\uD83D\uDCB8",
            label = stringResource(Res.string.analytics_stat_total_spent),
            value = state.totalSpentFormatted.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            emoji = "\uD83D\uDCC1",
            label = stringResource(Res.string.analytics_stat_categories),
            value = "${state.categoriesCount}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            emoji = "\u2705",
            label = stringResource(Res.string.analytics_stat_active),
            value = "${state.activeCategoriesCount}",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

// region Budget Pulse

@Composable
private fun BudgetPulseCard(burn: BudgetBurnUi) {
    val appColors = LocalAppColors.current
    val burnColor by animateColorAsState(
        targetValue = when {
            burn.percentage < BURN_THRESHOLD_GREEN -> appColors.budgetBurnGreen
            burn.percentage < BURN_THRESHOLD_YELLOW -> appColors.budgetBurnYellow
            else -> appColors.budgetBurnRed
        },
        label = "burnColor",
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.analytics_budget_pulse),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { burn.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BUDGET_BAR_HEIGHT.dp)
                    .clip(RoundedCornerShape(7.dp)),
                color = burnColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        Res.string.analytics_budget_spent_of,
                        burn.spentFormatted,
                        burn.budgetFormatted,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        Res.string.analytics_daily_allowance,
                        burn.dailyAllowanceFormatted,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = burnColor,
                )
            }
        }
    }
}

// endregion

// region Donut Pie Chart (Smart Legend)

@Composable
private fun SpendingOverviewCard(
    slices: ImmutableList<SpendingSlice>,
    pieModelProducer: PieChartModelProducer,
    shimmerOffset: Float,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shimmerOverlay(shimmerOffset, Color.White.copy(alpha = 0.04f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.analytics_spending_overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            PieChartHost(
                chart = rememberPieChart(
                    sliceProvider = PieChart.SliceProvider.series(
                        slices.map { slice ->
                            PieChart.Slice(fill = Fill(slice.color))
                        },
                    ),
                    innerSize = PieSize.Inner.fixed(80.dp),
                ),
                modelProducer = pieModelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            val avgDailySuffix = stringResource(Res.string.analytics_avg_daily)
            slices.forEach { slice ->
                SmartLegendRow(
                    color = slice.color,
                    name = slice.name,
                    value = slice.formattedAmount,
                    avgDaily = slice.formattedAvgDaily + avgDailySuffix,
                )
            }
        }
    }
}

// endregion

// region Monthly Comparison Bar Chart (with Projected Total)

@Composable
private fun MonthlyComparisonCard(
    monthlyBars: ImmutableList<MonthlyBarUiItem>,
    projectedTotal: ProjectedTotalUi?,
    monthlyModelProducer: CartesianChartModelProducer,
) {
    val labels = monthlyBars.map { it.label }
    val monthFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            labels.getOrElse(value.toInt()) { value.toInt().toString() }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.analytics_monthly_comparison),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = monthFormatter),
                ),
                modelProducer = monthlyModelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )

            projectedTotal?.let { projected ->
                Spacer(modifier = Modifier.height(12.dp))
                ProjectedTotalInsight(projected)
            }
        }
    }
}

@Composable
private fun ProjectedTotalInsight(projected: ProjectedTotalUi) {
    val appColors = LocalAppColors.current
    val color = if (projected.isOverBudget) {
        appColors.budgetBurnRed
    } else {
        appColors.budgetBurnGreen
    }

    Column {
        Text(
            text = stringResource(
                Res.string.analytics_projected_on_track,
                projected.projectedFormatted,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val overUnderRes = if (projected.isOverBudget) {
            Res.string.analytics_projected_over
        } else {
            Res.string.analytics_projected_under
        }
        Text(
            text = stringResource(
                overUnderRes,
                projected.overUnderFormatted,
                projected.projectedFormatted,
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

// endregion

// region Daily Spending Line Chart (with Ghost Line)

@Composable
private fun DailyTrendsCard(
    dailySpending: ImmutableList<DailySpendingUiPoint>,
    hasBudgetPacing: Boolean,
    dailyModelProducer: CartesianChartModelProducer,
) {
    val dayLabels = dailySpending.map { it.dayOfMonth.toString() }
    val dayFormatter = remember(dayLabels) {
        CartesianValueFormatter { _, value, _ ->
            dayLabels.getOrElse(value.toInt()) { value.toInt().toString() }
        }
    }

    val appColors = LocalAppColors.current
    val lineProvider = if (hasBudgetPacing) {
        val spendingLine = LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(appColors.spendingLine)),
        )
        val ghostLine = LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(appColors.ghostLine)),
            stroke = LineCartesianLayer.LineStroke.Dashed(thickness = GhostLineThickness),
        )
        LineCartesianLayer.LineProvider.series(spendingLine, ghostLine)
    } else {
        null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.analytics_daily_trends),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val layer = if (lineProvider != null) {
                rememberLineCartesianLayer(lineProvider = lineProvider)
            } else {
                rememberLineCartesianLayer()
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    layer,
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dayFormatter),
                ),
                modelProducer = dailyModelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )

            if (hasBudgetPacing) {
                Spacer(modifier = Modifier.height(8.dp))
                ChartLegendRow()
            }
        }
    }
}

@Composable
private fun ChartLegendRow() {
    val appColors = LocalAppColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 3.dp)
                    .background(appColors.spendingLine, RoundedCornerShape(2.dp)),
            )
            Text(
                text = stringResource(Res.string.analytics_stat_total_spent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 2.dp)
                    .background(appColors.ghostLine, RoundedCornerShape(1.dp)),
            )
            Text(
                text = stringResource(Res.string.analytics_budget_pulse),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion

// region Category Progress

@Composable
private fun CategoryProgressCard(categoryProgress: ImmutableList<CategoryProgressUi>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.analytics_category_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            categoryProgress.forEach { item ->
                CategoryProgressRow(item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(item: CategoryProgressUi) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(PROGRESS_BAR_HEIGHT.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = item.color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

// endregion

// region Empty State

@Composable
private fun EmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\uD83D\uDCCA",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.analytics_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.analytics_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion

// region Shared components

@Composable
private fun SmartLegendRow(color: Color, name: String, value: String, avgDaily: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(LEGEND_DOT_CORNER_PERCENT))
                .background(color),
        )
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = avgDaily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun QuickStatCard(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// endregion
