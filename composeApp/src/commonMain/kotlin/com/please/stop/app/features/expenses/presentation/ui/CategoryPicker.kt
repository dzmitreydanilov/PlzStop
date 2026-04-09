package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.theme.LocalAppDimens
import com.please.stop.app.uicomponents.categoryEmojiForKey
import kotlinx.collections.immutable.ImmutableList

private const val COLUMNS = 2

@Composable
internal fun CategoryPicker(
    categories: ImmutableList<CategoryUiModel>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.small2),
        verticalArrangement = Arrangement.spacedBy(dimens.extraSmall),
    ) {
        categories.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.extraSmall),
            ) {
                row.forEach { category ->
                    CategoryTile(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: CategoryUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val tileScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        ),
    )

    // Cyan Nebula: glass panels instead of elevated cards — no drop shadows
    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = tileScale
            scaleY = tileScale
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                appColors.cardGlass
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        shape = RoundedCornerShape(dimens.radiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                appColors.cardGlassBorder,
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            )
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.small1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.expenseCategoryIconSize)
                    .clip(RoundedCornerShape(dimens.layoutGridGap))
                    .then(
                        if (isSelected) {
                            Modifier.background(brush = appColors.primaryGradient)
                        } else {
                            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = categoryEmojiForKey(category.iconKey),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(modifier = Modifier.width(dimens.extraSmall))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
