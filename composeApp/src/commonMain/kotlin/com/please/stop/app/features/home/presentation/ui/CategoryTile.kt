package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.home.presentation.HomeCategoryUiModel
import com.please.stop.app.uicomponents.CategoryIconImage
import com.please.stop.app.uicomponents.previews.ApplicationPreviewThemeWrapper

private const val PRIMARY_CATEGORY_VARIANT = 0L
private const val SECONDARY_CATEGORY_VARIANT = 1L
private const val CATEGORY_VARIANT_COUNT = 3L

@Composable
internal fun CategoryTile(
    category: HomeCategoryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (iconContainerColor, iconContentColor) = when (category.id % CATEGORY_VARIANT_COUNT) {
        PRIMARY_CATEGORY_VARIANT -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        SECONDARY_CATEGORY_VARIANT -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        else -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                CategoryIconImage(
                    iconKey = category.iconKey,
                    tint = iconContentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = category.spentFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (category.hasSpending) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CategoryTilePreview() {
    CategoryTile(
        category = HomeCategoryUiModel(
            id = 1,
            name = "Food",
            iconKey = "ic_food",
            spentFormatted = "$120.50",
            hasSpending = true,
        ),
        onClick = {},
    )
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CategoryTileNoSpendingPreview() {
    CategoryTile(
        category = HomeCategoryUiModel(
            id = 2,
            name = "Entertainment",
            iconKey = "ic_entertainment",
            spentFormatted = "$0.00",
            hasSpending = false,
        ),
        onClick = {},
    )
}
