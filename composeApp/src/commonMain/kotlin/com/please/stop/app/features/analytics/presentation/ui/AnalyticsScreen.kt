package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshoe.charty.bar.BarChart
import com.himanshoe.charty.bar.model.BarData
import com.himanshoe.charty.pie.PieChart
import com.himanshoe.charty.pie.model.PieChartData
import com.please.stop.app.features.analytics.presentation.AnalyticsState
import com.please.stop.app.features.analytics.presentation.AnalyticsStateHolder
import com.please.stop.app.features.analytics.presentation.LegendItem
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.animation.FadeSlideIn
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_tab

@Composable
fun AnalyticsScreen() {
    val stateHolder = koinViewModel<AnalyticsStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    val appColors = LocalAppColors.current

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
                    text = "Insights & Reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is AnalyticsState.Content -> DashboardContent(state = s)
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading insights...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DashboardContent(state: AnalyticsState.Content) {
    val shimmerOffset = rememberShimmerOffset()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FadeSlideIn(delayMillis = 100) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickStatCard(
                    emoji = "\uD83D\uDCB8",
                    label = "Total Spent",
                    value = state.totalSpentFormatted.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                QuickStatCard(
                    emoji = "\uD83D\uDCC1",
                    label = "Categories",
                    value = "${state.categoriesCount}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                QuickStatCard(
                    emoji = "\u2705",
                    label = "Active",
                    value = "${state.activeCategoriesCount}",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.hasAnyExpenses && state.pieChartData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            FadeSlideIn(delayMillis = 250) {
                SpendingOverviewCard(
                    pieChartData = state.pieChartData,
                    legend = state.legend,
                    shimmerOffset = shimmerOffset,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            FadeSlideIn(delayMillis = 400) {
                CategoryBreakdownCard(barChartData = state.barChartData)
            }
        }

        if (!state.hasAnyExpenses) {
            Spacer(modifier = Modifier.height(20.dp))

            FadeSlideIn(delayMillis = 200) {
                EmptyStateCard()
            }
        }
    }
}

@Composable
private fun SpendingOverviewCard(
    pieChartData: ImmutableList<PieChartData>,
    legend: ImmutableList<LegendItem>,
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
                text = "Spending Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val pieFraction = animateChartFraction(pieChartData)

            PieChart(
                data = {
                    pieChartData.map { it.copy(value = it.value * pieFraction) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                isDonutChart = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            legend.forEach { item ->
                LegendRow(
                    color = item.color,
                    name = item.name,
                    value = item.value,
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(barChartData: ImmutableList<BarData>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val barFraction = animateChartFraction(barChartData)

            BarChart(
                data = {
                    barChartData.map { it.copy(yValue = it.yValue * barFraction) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
        }
    }
}

private const val MIN_CHART_FRACTION = 0.01f

@Composable
private fun animateChartFraction(key: Any): Float {
    var fraction = remember { mutableFloatStateOf(MIN_CHART_FRACTION) }

    LaunchedEffect(key) {
        val animatable = Animatable(MIN_CHART_FRACTION)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        ) {
            fraction.value = value
        }
    }

    return fraction.value
}

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
                text = "No spending data yet",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add your first expense to see charts and insights here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendRow(color: Color, name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
