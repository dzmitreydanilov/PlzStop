package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.MAX_NOTES_LENGTH
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.MAX_TITLE_LENGTH
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.NOTES_COUNTER_THRESHOLD
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.TITLE_COUNTER_THRESHOLD
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.uicomponents.CategoryIconImage
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_category
import plzstop.composeapp.generated.resources.add_expense_done
import plzstop.composeapp.generated.resources.add_expense_notes_label
import plzstop.composeapp.generated.resources.add_expense_title_label
import plzstop.composeapp.generated.resources.add_expense_what_was_it_for

private const val CATEGORY_GRID_COLUMNS = 3

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
            text = title.ifBlank { stringResource(Res.string.add_expense_what_was_it_for) },
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
                    CategoryIconImage(
                        iconKey = category.iconKey,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp),
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
