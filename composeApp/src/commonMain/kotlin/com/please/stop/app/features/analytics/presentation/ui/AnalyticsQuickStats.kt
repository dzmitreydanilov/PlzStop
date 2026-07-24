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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.analytics.presentation.AnalyticsState
import com.please.stop.app.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_empty_hint
import plzstop.composeapp.generated.resources.analytics_empty_title
import plzstop.composeapp.generated.resources.analytics_stat_active
import plzstop.composeapp.generated.resources.analytics_stat_categories
import plzstop.composeapp.generated.resources.analytics_stat_total_spent
import plzstop.composeapp.generated.resources.ic_analytics_outlined
import plzstop.composeapp.generated.resources.ic_archive
import plzstop.composeapp.generated.resources.ic_savings
import plzstop.composeapp.generated.resources.ic_success_circle

@Composable
internal fun QuickStatRow(state: AnalyticsState.Content) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickStatCard(
            icon = Res.drawable.ic_savings,
            label = stringResource(Res.string.analytics_stat_total_spent),
            value = state.totalSpentFormatted.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            icon = Res.drawable.ic_archive,
            label = stringResource(Res.string.analytics_stat_categories),
            value = "${state.categoriesCount}",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        QuickStatCard(
            icon = Res.drawable.ic_success_circle,
            label = stringResource(Res.string.analytics_stat_active),
            value = "${state.activeCategoriesCount}",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun QuickStatCard(
    icon: DrawableResource,
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
                Icon(
                    imageVector = vectorResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color,
                )
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

@Composable
internal fun EmptyStateCard() {
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
            Icon(
                imageVector = vectorResource(Res.drawable.ic_analytics_outlined),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
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

@Preview(showBackground = true)
@Composable
private fun QuickStatCardPreview() {
    AppTheme {
        QuickStatCard(
            icon = Res.drawable.ic_savings,
            label = "Total Spent",
            value = "$1,234",
            color = Color.Red,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateCardPreview() {
    AppTheme {
        EmptyStateCard()
    }
}
