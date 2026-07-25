package com.please.stop.app.features.categories.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.categories.presentation.CategoryRowUiModel
import com.please.stop.app.features.categories.presentation.SubcategoryChipUiModel
import com.please.stop.app.uicomponents.CategoryIconImage
import com.please.stop.app.uicomponents.previews.ApplicationPreviewThemeWrapper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add
import plzstop.composeapp.generated.resources.categories_hide_subcategories
import plzstop.composeapp.generated.resources.categories_show_subcategories
import plzstop.composeapp.generated.resources.content_desc_add_subcategory
import plzstop.composeapp.generated.resources.content_desc_archive_subcategory
import plzstop.composeapp.generated.resources.content_desc_delete_subcategory
import plzstop.composeapp.generated.resources.content_desc_edit_category
import plzstop.composeapp.generated.resources.ic_add
import plzstop.composeapp.generated.resources.ic_archive
import plzstop.composeapp.generated.resources.ic_delete
import plzstop.composeapp.generated.resources.ic_edit
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_down

private const val MAX_SUBCATEGORIES = 10
private const val EXPANDED_CHEVRON_ROTATION = 180f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryManagementRow(
    category: CategoryRowUiModel,
    onExpandSubcategories: () -> Unit,
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
                ExpandableSubcategories(
                    subcategories = subcategories,
                    subcategoryCount = category.subcategoryCount ?: 0,
                    onExpand = onExpandSubcategories,
                    onAddSubcategoryClick = onAddSubcategoryClick,
                    onDeleteSubcategoryClick = onDeleteSubcategoryClick,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableSubcategories(
    subcategories: ImmutableList<SubcategoryChipUiModel>,
    subcategoryCount: Int,
    onExpand: () -> Unit,
    onAddSubcategoryClick: () -> Unit,
    onDeleteSubcategoryClick: (SubcategoryChipUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) EXPANDED_CHEVRON_ROTATION else 0f,
        label = "subcategory chevron",
    )

    Column(modifier = modifier) {
        if (subcategoryCount > 0) {
            TextButton(
                onClick = {
                    expanded = !expanded
                    if (expanded) onExpand()
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (expanded) {
                            Res.string.categories_hide_subcategories
                        } else {
                            Res.string.categories_show_subcategories
                        },
                        subcategoryCount,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded || subcategoryCount == 0,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                subcategories.forEach { subcategory ->
                    SubcategoryChip(
                        subcategory = subcategory,
                        onDeleteClick = { onDeleteSubcategoryClick(subcategory) },
                    )
                }

                if (subcategoryCount < MAX_SUBCATEGORIES) {
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

@Composable
private fun SubcategoryChip(
    subcategory: SubcategoryChipUiModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuggestionChip(
        onClick = onDeleteClick,
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
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CategoryManagementRowPreview() {
    CategoryManagementRow(
        category = CategoryRowUiModel(
            id = 1L,
            name = "Food",
            iconKey = "ic_food",
            comment = "Groceries & dining out",
            subcategoryCount = 2,
            subcategories = persistentListOf(
                SubcategoryChipUiModel(id = 10L, name = "Groceries", comment = null),
                SubcategoryChipUiModel(id = 11L, name = "Restaurants", comment = "Eating out"),
            ),
        ),
        onExpandSubcategories = {},
        onAddSubcategoryClick = {},
        onDeleteSubcategoryClick = {},
        onEditClick = {},
        onDeleteClick = {},
    )
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun CategoryManagementRowNoSubcategoriesPreview() {
    CategoryManagementRow(
        category = CategoryRowUiModel(
            id = 2L,
            name = "Transport",
            iconKey = "ic_transport",
            comment = null,
            subcategoryCount = null,
            subcategories = null,
        ),
        onExpandSubcategories = {},
        onAddSubcategoryClick = {},
        onDeleteSubcategoryClick = {},
        onEditClick = {},
        onDeleteClick = {},
    )
}
