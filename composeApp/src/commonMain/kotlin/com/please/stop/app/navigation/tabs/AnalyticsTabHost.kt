package com.please.stop.app.navigation.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.features.home.presentation.HomeState
import com.please.stop.app.features.home.presentation.HomeStateHolder
import com.please.stop.app.navigation.routes.MainBottomTabs
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.animation.FadeSlideIn
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.animation.shimmerOverlay
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_tab

internal fun EntryProviderScope<NavKey>.analyticsTabEntries() {
    entry<MainBottomTabs.Analytics> {
        DashboardScreen()
    }
}

@Composable
private fun DashboardScreen() {
    val stateHolder = koinViewModel<HomeStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FadeSlideIn {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(appColors.headerGradient)
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.analytics_tab),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Insights & Reports",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is HomeState.Content -> DashboardContent(state = s)
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading insights...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DashboardContent(state: HomeState.Content) {
    val appColors = LocalAppColors.current
    val shimmerOffset = rememberShimmerOffset()
    val categories = state.categories
    val total = state.totalSpentFormatted
    val hasExpenses = state.hasAnyExpenses

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FadeSlideIn(delayMillis = 100) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickStatCard(
                    emoji = "\uD83D\uDCB8",
                    label = "Total Spent",
                    value = total,
                    color = appColors.rose500,
                    modifier = Modifier.weight(1f),
                )
                QuickStatCard(
                    emoji = "\uD83D\uDCC1",
                    label = "Categories",
                    value = "${categories.size}",
                    color = appColors.blue500,
                    modifier = Modifier.weight(1f),
                )
                QuickStatCard(
                    emoji = "\u2705",
                    label = "Active",
                    value = "${categories.count { it.hasSpending }}",
                    color = appColors.teal500,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (hasExpenses && categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            FadeSlideIn(delayMillis = 250) {
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
                        Text(
                            text = "Spending Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val spendingCategories = categories.filter { it.hasSpending }
                        val chartColors = listOf(
                            appColors.teal500, appColors.blue500, appColors.purple500,
                            appColors.pink500, appColors.amber500, appColors.emerald500,
                        )

                        AnimatedDonutChart(
                            items = spendingCategories.map { it.name },
                            colors = spendingCategories.mapIndexed { i, _ ->
                                chartColors[i % chartColors.size]
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .aspectRatio(1f)
                                .align(Alignment.CenterHorizontally),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        spendingCategories.forEachIndexed { i, cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(chartColors[i % chartColors.size]),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = cat.spentFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            FadeSlideIn(delayMillis = 400) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val spendingCategories = categories.filter { it.hasSpending }
                        spendingCategories.forEachIndexed { i, cat ->
                            val chartColors = listOf(
                                appColors.teal500, appColors.blue500, appColors.purple500,
                                appColors.pink500, appColors.amber500, appColors.emerald500,
                            )

                            AnimatedCategoryBar(
                                name = cat.name,
                                value = cat.spentFormatted,
                                color = chartColors[i % chartColors.size],
                                fraction = if (spendingCategories.size <= 1) 1f
                                else 1f - (i.toFloat() / spendingCategories.size * 0.6f),
                                delayMillis = 400 + i * 80,
                            )
                            if (i < spendingCategories.lastIndex) {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        }

        if (!hasExpenses) {
            Spacer(modifier = Modifier.height(20.dp))

            FadeSlideIn(delayMillis = 200) {
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
                        Text(
                            text = "\uD83D\uDCCA",
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No spending data yet",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add your first expense to see charts and insights here.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    emoji: String,
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
                Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
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
private fun AnimatedDonutChart(
    items: List<String>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val sweepAngle = remember { Animatable(0f) }

    LaunchedEffect(items) {
        sweepAngle.snapTo(0f)
        sweepAngle.animateTo(360f, tween(1000, easing = FastOutSlowInEasing))
    }

    val count = items.size.coerceAtLeast(1)
    val segmentAngle = 360f / count

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.18f
        val radius = (size.minDimension - strokeWidth) / 2f
        val topLeft = Offset(
            (size.width - radius * 2) / 2f,
            (size.height - radius * 2) / 2f,
        )
        val arcSize = Size(radius * 2, radius * 2)

        var startAngle = -90f
        for (i in items.indices) {
            val thisSweep = (segmentAngle).coerceAtMost(sweepAngle.value - (i * segmentAngle)).coerceIn(0f, segmentAngle)
            if (thisSweep > 0f) {
                drawArc(
                    color = colors[i % colors.size],
                    startAngle = startAngle,
                    sweepAngle = thisSweep - 3f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            startAngle += segmentAngle
        }
    }
}

@Composable
private fun AnimatedCategoryBar(
    name: String,
    value: String,
    color: Color,
    fraction: Float,
    delayMillis: Int,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(name) {
        delay(delayMillis.toLong())
        progress.animateTo(fraction, tween(600, easing = FastOutSlowInEasing))
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}
