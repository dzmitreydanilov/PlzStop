package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.please.stop.app.features.analytics.presentation.SpendingSlice
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_avg_daily
import plzstop.composeapp.generated.resources.analytics_spending_overview

private const val CHART_HEIGHT = 220
private const val LEGEND_DOT_CORNER_PERCENT = 50

@Composable
internal fun SpendingOverviewCard(
    slices: ImmutableList<SpendingSlice>,
    shimmerOffset: Float,
) {
    val pieModelProducer = remember { PieChartModelProducer() }
    val sliceAmounts = slices.map { it.amount }
    LaunchedEffect(sliceAmounts) {
        pieModelProducer.runTransaction {
            pieSeries { series(sliceAmounts) }
        }
    }

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
            val chartColors = LocalAppColors.current.chartColors

            Text(
                text = stringResource(Res.string.analytics_spending_overview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            PieChartHost(
                chart = rememberPieChart(
                    sliceProvider = PieChart.SliceProvider.series(
                        slices.mapIndexed { i, _ ->
                            PieChart.Slice(fill = Fill(chartColors[i % chartColors.size]))
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
            slices.forEachIndexed { i, slice ->
                SmartLegendRow(
                    color = chartColors[i % chartColors.size],
                    name = slice.name,
                    value = slice.formattedAmount,
                    avgDaily = slice.formattedAvgDaily + avgDailySuffix,
                )
            }
        }
    }
}

@Composable
internal fun SmartLegendRow(color: Color, name: String, value: String, avgDaily: String) {
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

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun SmartLegendRowPreview() {
    AppTheme {
        SmartLegendRow(
            color = LocalAppColors.current.chartColors.first(),
            name = "Food & Dining",
            value = "$450",
            avgDaily = "$15/day",
        )
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun SpendingOverviewCardPreview() {
    AppTheme {
        SpendingOverviewCard(
            slices = persistentListOf(
                SpendingSlice("Food", "$450", "$15/day", 450f),
                SpendingSlice("Transport", "$200", "$7/day", 200f),
                SpendingSlice("Entertainment", "$150", "$5/day", 150f),
            ),
            shimmerOffset = 0f,
        )
    }
}
