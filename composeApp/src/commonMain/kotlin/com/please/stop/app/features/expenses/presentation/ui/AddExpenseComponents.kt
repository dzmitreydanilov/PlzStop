package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.MAX_NOTES_LENGTH
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.MAX_TITLE_LENGTH
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.NOTES_COUNTER_THRESHOLD
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.TITLE_COUNTER_THRESHOLD
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.uicomponents.categoryEmojiForKey
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_analyzing_receipt
import plzstop.composeapp.generated.resources.add_expense_category
import plzstop.composeapp.generated.resources.add_expense_done
import plzstop.composeapp.generated.resources.add_expense_notes_label
import plzstop.composeapp.generated.resources.add_expense_scan_receipt
import plzstop.composeapp.generated.resources.add_expense_scan_receipt_hint
import plzstop.composeapp.generated.resources.add_expense_select_category
import plzstop.composeapp.generated.resources.add_expense_title_label
import plzstop.composeapp.generated.resources.add_expense_what_was_it_for
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_right
import plzstop.composeapp.generated.resources.ic_scan

private val SCAN_LINE_TRAVEL_PX = 40.dp
private const val CATEGORY_GRID_COLUMNS = 3

@Composable
internal fun ScanReceiptCard(
    isAnalyzing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanLineOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1500,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = !isAnalyzing,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = vectorResource(Res.drawable.ic_scan),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.add_expense_scan_receipt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (isAnalyzing) {
                            stringResource(Res.string.add_expense_analyzing_receipt)
                        } else {
                            stringResource(Res.string.add_expense_scan_receipt_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isAnalyzing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(width = 32.dp, height = 2.dp)
                        .graphicsLayer {
                            translationY = (scanLineOffset - 0.5f) * SCAN_LINE_TRAVEL_PX.toPx()
                        }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
    }
}

@Composable
internal fun CategoryChip(
    selectedCategory: CategoryUiModel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = selectedCategory?.let {
                    "${categoryEmojiForKey(it.iconKey)} ${it.name}"
                } ?: stringResource(Res.string.add_expense_select_category),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = null,
        elevation = AssistChipDefaults.assistChipElevation(elevation = 2.dp),
    )
}

@Composable
internal fun NotesSection(
    title: String,
    notes: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = if (title.isNotBlank()) title else stringResource(Res.string.add_expense_what_was_it_for),
            style = MaterialTheme.typography.headlineSmall,
            color = if (title.isNotBlank()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            },
        )
        if (notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryGridSheet(
    categories: ImmutableList<CategoryUiModel>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(Res.string.add_expense_category),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(CATEGORY_GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                val isSelected = category.id == selectedCategoryId
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                        .clickable { onSelectCategory(category.id) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = categoryEmojiForKey(category.iconKey),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun NotesInputSheet(
    title: String,
    notes: String,
    onChangeTitle: (String) -> Unit,
    onChangeNotes: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(Res.string.add_expense_what_was_it_for),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        OutlinedTextField(
            value = title,
            onValueChange = { onChangeTitle(it.take(MAX_TITLE_LENGTH)) },
            label = { Text(stringResource(Res.string.add_expense_title_label)) },
            singleLine = true,
            supportingText = if (title.length > TITLE_COUNTER_THRESHOLD) {
                { Text("${title.length}/$MAX_TITLE_LENGTH") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { onChangeNotes(it.take(MAX_NOTES_LENGTH)) },
            label = { Text(stringResource(Res.string.add_expense_notes_label)) },
            minLines = 3,
            maxLines = 6,
            supportingText = if (notes.length > NOTES_COUNTER_THRESHOLD) {
                { Text("${notes.length}/$MAX_NOTES_LENGTH") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onDone,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(Res.string.add_expense_done))
        }
    }
}
