package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.home.presentation.HomeCategoryUiModel
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.theme.LocalAppDimens
import com.please.stop.app.uicomponents.categoryEmojiForKey

@Composable
internal fun CategoryTile(
    category: HomeCategoryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val gradients = appColors.categoryGradients
    val gradientIndex = category.name.hashCode()
        .let { (it % gradients.size + gradients.size) % gradients.size }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "CategoryTileScale",
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "CategoryTileTranslateY",
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 1.dp else 4.dp,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = animatedTranslationY
            }
            .border(
                width = 1.dp,
                color = appColors.cardGlassBorder,
                shape = MaterialTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.small1),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.homeCategoryIconSize)
                    .clip(MaterialTheme.shapes.small)
                    .background(gradients[gradientIndex]),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = categoryEmojiForKey(category.iconKey),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(dimens.extraSmall))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(dimens.xxSmall))
            Text(
                text = category.spentFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = if (category.hasSpending) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (category.hasSpending) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryTilePreview() {
    AppTheme {
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
}

@Preview(showBackground = true)
@Composable
private fun CategoryTileNoSpendingPreview() {
    AppTheme {
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
}
