package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.please.stop.app.features.analytics.presentation.HeatmapDayUi
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_heatmap_high
import plzstop.composeapp.generated.resources.analytics_heatmap_low
import plzstop.composeapp.generated.resources.analytics_spending_frequency
import plzstop.composeapp.generated.resources.day_of_week_short

private const val COLUMNS = 7
private const val CELL_CORNER_RADIUS_PX = 8f
private const val CELL_SPACING_DP = 6
private const val LEGEND_STEPS = 4
private const val DAY_LABEL_FONT_SIZE = 10
private const val INTENSITY_TEXT_THRESHOLD = 0.5f

@Composable
internal fun SpendingHeatmapCard(
    heatmapDays: ImmutableList<HeatmapDayUi>,
    accentColor: Color,
    onDayTap: (dayOfMonth: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowCount = (heatmapDays.maxOfOrNull { it.weekOfMonth } ?: 0) + 1
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val dayLabels = stringArrayResource(Res.array.day_of_week_short)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.analytics_spending_frequency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val textMeasurer = rememberTextMeasurer()
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val labelHighColor = MaterialTheme.colorScheme.onPrimary
            val labelStyle = remember {
                TextStyle(fontSize = DAY_LABEL_FONT_SIZE.sp, textAlign = TextAlign.Center)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                val spacingDp = CELL_SPACING_DP.dp
                val cellSizeDp = (maxWidth - spacingDp * (COLUMNS - 1)) / COLUMNS
                val gridHeight = cellSizeDp * rowCount + spacingDp * (rowCount - 1)

                val density = LocalDensity.current
                val cellPx = remember(cellSizeDp) { with(density) { cellSizeDp.toPx() } }
                val spacingPx = remember(spacingDp) { with(density) { spacingDp.toPx() } }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .pointerInput(heatmapDays) {
                            detectTapGestures { offset ->
                                val step = cellPx + spacingPx
                                val col = (offset.x / step).toInt().coerceIn(0, COLUMNS - 1)
                                val row = (offset.y / step).toInt()
                                val tappedDay = heatmapDays.firstOrNull {
                                    it.dayOfWeek - 1 == col && it.weekOfMonth == row
                                }
                                tappedDay?.let { onDayTap(it.dayOfMonth) }
                            }
                        },
                ) {
                    val spacing = spacingDp.toPx()
                    val cellSize = cellSizeDp.toPx()

                    for (day in heatmapDays) {
                        val col = day.dayOfWeek - 1
                        val row = day.weekOfMonth
                        val x = col * (cellSize + spacing)
                        val y = row * (cellSize + spacing)

                        val cellColor = if (day.intensity > 0f) {
                            lerp(surfaceColor, accentColor, day.intensity)
                        } else {
                            surfaceColor
                        }

                        drawRoundRect(
                            color = cellColor,
                            topLeft = Offset(x, y),
                            size = Size(cellSize, cellSize),
                            cornerRadius = CornerRadius(CELL_CORNER_RADIUS_PX, CELL_CORNER_RADIUS_PX),
                        )

                        val textColor = if (day.intensity > INTENSITY_TEXT_THRESHOLD) {
                            labelHighColor
                        } else {
                            labelColor
                        }
                        val textResult = textMeasurer.measure(
                            text = day.dayOfMonth.toString(),
                            style = labelStyle.copy(color = textColor),
                        )
                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(
                                x = x + (cellSize - textResult.size.width) / 2f,
                                y = y + (cellSize - textResult.size.height) / 2f,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            IntensityLegend(surfaceColor = surfaceColor, accentColor = accentColor)
        }
    }
}

@Composable
private fun IntensityLegend(surfaceColor: Color, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.analytics_heatmap_low),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.padding(start = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(LEGEND_STEPS) { i ->
                val fraction = (i + 1).toFloat() / LEGEND_STEPS
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(lerp(surfaceColor, accentColor, fraction)),
                )
            }
        }
        Spacer(modifier = Modifier.padding(start = 6.dp))
        Text(
            text = stringResource(Res.string.analytics_heatmap_high),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
