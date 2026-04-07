package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.please.stop.app.features.analytics.presentation.DailySpendingUiPoint
import com.please.stop.app.theme.LocalAppColors
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_budget_pulse
import plzstop.composeapp.generated.resources.analytics_daily_trends
import plzstop.composeapp.generated.resources.analytics_stat_total_spent

private val GhostLineThickness = 1.5.dp

@Composable
internal fun DailyTrendsCard(
    dailySpending: ImmutableList<DailySpendingUiPoint>,
    hasBudgetPacing: Boolean,
    budgetPacingPoints: List<Float>,
) {
    val dailyModelProducer = remember { CartesianChartModelProducer() }
    val dailyAmounts = dailySpending.map { it.amount }
    LaunchedEffect(dailyAmounts, budgetPacingPoints) {
        if (dailyAmounts.isEmpty()) return@LaunchedEffect
        dailyModelProducer.runTransaction {
            lineSeries {
                series(dailyAmounts)
                if (budgetPacingPoints.isNotEmpty()) {
                    series(budgetPacingPoints)
                }
            }
        }
    }

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
