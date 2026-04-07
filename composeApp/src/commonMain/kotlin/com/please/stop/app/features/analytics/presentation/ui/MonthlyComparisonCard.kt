package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.please.stop.app.features.analytics.presentation.MonthlyBarUiItem
import com.please.stop.app.features.analytics.presentation.ProjectedTotalUi
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_monthly_comparison
import plzstop.composeapp.generated.resources.analytics_projected_on_track
import plzstop.composeapp.generated.resources.analytics_projected_over
import plzstop.composeapp.generated.resources.analytics_projected_under

@Composable
internal fun MonthlyComparisonCard(
    monthlyBars: ImmutableList<MonthlyBarUiItem>,
    projectedTotal: ProjectedTotalUi?,
) {
    val monthlyModelProducer = remember { CartesianChartModelProducer() }
    val barAmounts = monthlyBars.map { it.amount }
    LaunchedEffect(barAmounts) {
        if (barAmounts.isEmpty()) return@LaunchedEffect
        monthlyModelProducer.runTransaction {
            columnSeries { series(barAmounts) }
        }
    }

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

@Preview(showBackground = true)
@Composable
private fun ProjectedTotalInsightOverPreview() {
    AppTheme {
        ProjectedTotalInsight(
            projected = ProjectedTotalUi(
                projectedFormatted = "$2,400",
                overUnderFormatted = "$400",
                isOverBudget = true,
            ),
        )
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun MonthlyComparisonCardPreview() {
    AppTheme {
        MonthlyComparisonCard(
            monthlyBars = persistentListOf(
                MonthlyBarUiItem("Nov", 1200f, "$1,200", false),
                MonthlyBarUiItem("Dec", 1800f, "$1,800", false),
                MonthlyBarUiItem("Jan", 1500f, "$1,500", false),
                MonthlyBarUiItem("Feb", 1100f, "$1,100", false),
                MonthlyBarUiItem("Mar", 1600f, "$1,600", false),
                MonthlyBarUiItem("Apr", 900f, "$900", true),
            ),
            projectedTotal = ProjectedTotalUi(
                projectedFormatted = "$1,800",
                overUnderFormatted = "$200",
                isOverBudget = false,
            ),
        )
    }
}
