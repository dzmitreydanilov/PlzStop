package com.please.stop.app.features.categories.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.categories.presentation.CategoryRowUiModel
import com.please.stop.app.features.categories.presentation.SubcategoryChipUiModel
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.uicomponents.CategoryIconImage
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add
import plzstop.composeapp.generated.resources.content_desc_add_subcategory
import plzstop.composeapp.generated.resources.content_desc_archive_subcategory
import plzstop.composeapp.generated.resources.content_desc_delete_subcategory
import plzstop.composeapp.generated.resources.content_desc_edit_category
import plzstop.composeapp.generated.resources.ic_add
import plzstop.composeapp.generated.resources.ic_archive
import plzstop.composeapp.generated.resources.ic_delete
import plzstop.composeapp.generated.resources.ic_edit

private const val MAX_SUBCATEGORIES = 10

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryManagementRow(
    category: CategoryRowUiModel,
    onAddSubcategoryClick: () -> Unit,
    onDeleteSubcategoryClick: (SubcategoryChipUiModel) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryIconImage(
                    iconKey = category.iconKey,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_edit),
                        contentDescription = stringResource(Res.string.content_desc_edit_category),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_archive),
                        contentDescription = stringResource(Res.string.content_desc_archive_subcategory),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (!category.comment.isNullOrBlank()) {
                Text(
                    text = category.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val subcategories = category.subcategories
            if (subcategories != null) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    subcategories.forEach { subcategory ->
                        SuggestionChip(
                            onClick = { onDeleteSubcategoryClick(subcategory) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(text = subcategory.name)
                                        if (!subcategory.comment.isNullOrBlank()) {
                                            Text(
                                                text = subcategory.comment,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = vectorResource(Res.drawable.ic_delete),
                                        contentDescription = stringResource(
                                            Res.string.content_desc_delete_subcategory,
                                            subcategory.name,
                                        ),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            },
                        )
                    }

                    if (subcategories.size < MAX_SUBCATEGORIES) {
                        AssistChip(
                            onClick = onAddSubcategoryClick,
                            label = { Text(text = stringResource(Res.string.add)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_add),
                                    contentDescription = stringResource(
                                        Res.string.content_desc_add_subcategory,
                                    ),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryManagementRowPreview() {
    AppTheme {
        CategoryManagementRow(
            category = CategoryRowUiModel(
                id = 1L,
                name = "Food",
                iconKey = "ic_food",
                comment = "Groceries & dining out",
                subcategories = persistentListOf(
                    SubcategoryChipUiModel(id = 10L, name = "Groceries", comment = null),
                    SubcategoryChipUiModel(id = 11L, name = "Restaurants", comment = "Eating out"),
                ),
            ),
            onAddSubcategoryClick = {},
            onDeleteSubcategoryClick = {},
            onEditClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryManagementRowNoSubcategoriesPreview() {
    AppTheme {
        CategoryManagementRow(
            category = CategoryRowUiModel(
                id = 2L,
                name = "Transport",
                iconKey = "ic_transport",
                comment = null,
                subcategories = null,
            ),
            onAddSubcategoryClick = {},
            onDeleteSubcategoryClick = {},
            onEditClick = {},
            onDeleteClick = {},
        )
    }
}
