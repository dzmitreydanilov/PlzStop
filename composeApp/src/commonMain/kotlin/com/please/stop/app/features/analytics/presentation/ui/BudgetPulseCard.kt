package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.analytics.presentation.BudgetBurnUi
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_budget_pulse
import plzstop.composeapp.generated.resources.analytics_budget_spent_of
import plzstop.composeapp.generated.resources.analytics_daily_allowance

private const val BUDGET_BAR_HEIGHT = 14
private const val BURN_THRESHOLD_GREEN = 0.6f
private const val BURN_THRESHOLD_YELLOW = 0.85f

@Composable
internal fun BudgetPulseCard(burn: BudgetBurnUi) {
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

@Preview(showBackground = true)
@Composable
private fun BudgetPulseCardPreview() {
    AppTheme {
        BudgetPulseCard(
            burn = BudgetBurnUi(
                spentFormatted = "$1,234",
                budgetFormatted = "$2,000",
                percentage = 0.62f,
                dailyAllowanceFormatted = "$38",
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetPulseCardOverBudgetPreview() {
    AppTheme {
        BudgetPulseCard(
            burn = BudgetBurnUi(
                spentFormatted = "$2,100",
                budgetFormatted = "$2,000",
                percentage = 1.05f,
                dailyAllowanceFormatted = "$0",
            ),
        )
    }
}
