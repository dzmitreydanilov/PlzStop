package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.analytics.presentation.CategoryProgressUi
import com.please.stop.app.features.analytics.presentation.SubcategoryProgressUi
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_category_breakdown
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_down

private const val PROGRESS_BAR_HEIGHT = 8
private const val SUBCATEGORY_PROGRESS_BAR_HEIGHT = 6
private const val CHEVRON_ROTATION = 180f

@Composable
internal fun CategoryProgressCard(categoryProgress: ImmutableList<CategoryProgressUi>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val chartColors = LocalAppColors.current.chartColors

            Text(
                text = stringResource(Res.string.analytics_category_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            categoryProgress.forEachIndexed { i, item ->
                CategoryProgressRow(item, color = chartColors[i % chartColors.size])
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun CategoryProgressRow(item: CategoryProgressUi, color: Color) {
    val hasSubcategories = item.subcategories.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) CHEVRON_ROTATION else 0f,
        label = "chevron",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasSubcategories) Modifier.clickable { expanded = !expanded } else Modifier,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasSubcategories) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { item.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(PROGRESS_BAR_HEIGHT.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 8.dp)) {
                item.subcategories.forEach { sub ->
                    SubcategoryProgressRow(sub, parentColor = color)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun SubcategoryProgressRow(item: SubcategoryProgressUi, parentColor: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { item.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(SUBCATEGORY_PROGRESS_BAR_HEIGHT.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = parentColor.copy(alpha = 0.6f),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun CategoryProgressCardPreview() {
    AppTheme {
        CategoryProgressCard(
            categoryProgress = persistentListOf(
                CategoryProgressUi(
                    name = "Food & Dining",
                    iconKey = "ic_food",
                    percentage = 0.45f,
                    formattedAmount = "$450",
                    subcategories = persistentListOf(
                        SubcategoryProgressUi("Groceries", 0.6f, "$270"),
                        SubcategoryProgressUi("Restaurants", 0.3f, "$135"),
                        SubcategoryProgressUi("Coffee", 0.1f, "$45"),
                    ),
                ),
                CategoryProgressUi(
                    name = "Transport",
                    iconKey = "ic_transport",
                    percentage = 0.25f,
                    formattedAmount = "$250",
                ),
            ),
        )
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun CategoryProgressRowExpandedPreview() {
    AppTheme {
        CategoryProgressRow(
            item = CategoryProgressUi(
                name = "Food & Dining",
                iconKey = "ic_food",
                percentage = 0.45f,
                formattedAmount = "$450",
                subcategories = persistentListOf(
                    SubcategoryProgressUi("Groceries", 0.6f, "$270"),
                    SubcategoryProgressUi("Restaurants", 0.3f, "$135"),
                ),
            ),
            color = LocalAppColors.current.chartColors.first(),
        )
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true)
@Composable
private fun SubcategoryProgressRowPreview() {
    AppTheme {
        SubcategoryProgressRow(
            item = SubcategoryProgressUi("Groceries", 0.6f, "$270"),
            parentColor = LocalAppColors.current.chartColors.first(),
        )
    }
}
