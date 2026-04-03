package com.please.stop.app.features.home.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.home.presentation.HomeCategoryUiModel
import com.please.stop.app.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
internal fun CategoryTile(
    category: HomeCategoryUiModel,
    index: Int = 0,
    onClick: () -> Unit,
) {
    val appColors = LocalAppColors.current
    val gradients = appColors.categoryGradients
    val gradientIndex = category.name.hashCode()
        .let { (it % gradients.size + gradients.size) % gradients.size }

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(category.id) {
        delay(index * 50L)
        alpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(category.id) {
        delay(index * 50L)
        offsetY.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(category.id) {
        delay(index * 50L)
        scale.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
            scaleX = scale.value
            scaleY = scale.value
        },
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradients[gradientIndex]),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = categoryEmojiForKey(category.iconKey),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = category.spentFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = if (category.hasSpending) appColors.teal600
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (category.hasSpending) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}
