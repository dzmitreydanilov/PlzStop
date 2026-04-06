package com.please.stop.app.features.categories.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.please.stop.app.features.categories.presentation.CategoriesEvent
import com.please.stop.app.features.categories.presentation.CategoriesState
import com.please.stop.app.features.categories.presentation.CategoriesStateHolder
import com.please.stop.app.features.categories.presentation.CategoryDeletionDialog
import com.please.stop.app.features.categories.presentation.CategoryRowUiModel
import com.please.stop.app.features.categories.presentation.CategoryTargetUiModel
import com.please.stop.app.features.categories.presentation.SubcategoryChipUiModel
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.uicomponents.allCategoryIcons
import com.please.stop.app.uicomponents.buttons.ApplicationButton
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.error.SnackbarPosition
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import com.please.stop.app.uicomponents.sheets.AppModalBottomSheet
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.categories_add_category
import plzstop.composeapp.generated.resources.categories_add_subcategory
import plzstop.composeapp.generated.resources.categories_comment_label
import plzstop.composeapp.generated.resources.categories_confirm
import plzstop.composeapp.generated.resources.categories_delete
import plzstop.composeapp.generated.resources.categories_delete_confirm_message
import plzstop.composeapp.generated.resources.categories_delete_confirm_title
import plzstop.composeapp.generated.resources.categories_delete_delete_expenses
import plzstop.composeapp.generated.resources.categories_delete_has_expenses_message
import plzstop.composeapp.generated.resources.categories_delete_has_expenses_title
import plzstop.composeapp.generated.resources.categories_delete_move_expenses
import plzstop.composeapp.generated.resources.categories_delete_subcategory_message
import plzstop.composeapp.generated.resources.categories_delete_subcategory_title
import plzstop.composeapp.generated.resources.categories_edit_category
import plzstop.composeapp.generated.resources.categories_icon_label
import plzstop.composeapp.generated.resources.categories_name_label
import plzstop.composeapp.generated.resources.categories_save
import plzstop.composeapp.generated.resources.categories_screen_title
import plzstop.composeapp.generated.resources.content_desc_add_category
import plzstop.composeapp.generated.resources.content_desc_navigate_back
import plzstop.composeapp.generated.resources.ic_add
import plzstop.composeapp.generated.resources.ic_arrow_back

private const val DEFAULT_ICON_KEY = "ic_other"

@Composable
fun CategoriesScreen(onGoBack: () -> Unit) {
    val stateHolder = koinViewModel<CategoriesStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(CategoriesEvent.DismissError) },
        onAutoDismiss = { stateHolder.processEvent(CategoriesEvent.DismissError) },
    ) {
        DisplayFullScreenProgress(showProgress = state is CategoriesState.Loading)
        CategoriesContent(state = state, onEvent = stateHolder::processEvent, onGoBack = onGoBack)
    }
}

internal val CategoriesState.asOverlay: ScreenOverlay?
    @Composable get() {
        val message = successMessage
        return when {
            this is CategoriesState.Error -> ScreenOverlay.Error(type = errorType)
            message != null -> ScreenOverlay.Message(
                title = message,
                position = SnackbarPosition.Bottom,
            )
            else -> null
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesContent(
    state: CategoriesState,
    onEvent: (CategoriesEvent) -> Unit,
    onGoBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.categories_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onGoBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.content_desc_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(CategoriesEvent.AddCategoryClicked) }) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.content_desc_add_category),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = state.categories, key = { it.id }) { category ->
                CategoryManagementRow(
                    category = category,
                    onAddSubcategoryClick = {
                        onEvent(CategoriesEvent.AddSubcategoryClicked(category.id))
                    },
                    onDeleteSubcategoryClick = { subcategory ->
                        onEvent(CategoriesEvent.DeleteSubcategoryClicked(subcategory))
                    },
                    onEditClick = { onEvent(CategoriesEvent.EditCategoryClicked(category)) },
                    onDeleteClick = { onEvent(CategoriesEvent.DeleteCategoryClicked(category.id)) },
                )
            }
        }
    }

    CategoriesSheets(state = state, onEvent = onEvent)
    CategoriesDeletionDialogs(state = state, onEvent = onEvent)
    DeleteSubcategoryDialog(state = state, onEvent = onEvent)
}

@Composable
private fun CategoriesSheets(
    state: CategoriesState,
    onEvent: (CategoriesEvent) -> Unit,
) {
    if (state.showAddCategorySheet) {
        AddCategorySheet(
            onConfirm = { name, iconKey, comment ->
                onEvent(CategoriesEvent.ConfirmAddCategory(name, iconKey, comment))
            },
            onDismiss = { onEvent(CategoriesEvent.DismissAddCategorySheet) },
        )
    }

    val editingCategory = state.editingCategory
    if (editingCategory != null) {
        EditCategorySheet(
            category = editingCategory,
            onConfirm = { name, iconKey, comment ->
                onEvent(
                    CategoriesEvent.ConfirmEditCategory(editingCategory.id, name, iconKey, comment),
                )
            },
            onDismiss = { onEvent(CategoriesEvent.DismissEditCategorySheet) },
        )
    }

    val subcategoryCategoryId = state.addSubcategoryForCategoryId
    if (subcategoryCategoryId != null) {
        AddSubcategorySheet(
            onConfirm = { name, comment ->
                onEvent(CategoriesEvent.ConfirmAddSubcategory(subcategoryCategoryId, name, comment))
            },
            onDismiss = { onEvent(CategoriesEvent.DismissAddSubcategorySheet) },
        )
    }
}

@Composable
private fun AddCategorySheet(
    onConfirm: (name: String, iconKey: String, comment: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf(DEFAULT_ICON_KEY) }

    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded,
        detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
    )

    AppModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.categories_add_category),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.categories_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(Res.string.categories_comment_label)) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.categories_icon_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            EmojiPickerGrid(
                selectedIconKey = selectedIconKey,
                onSelectIcon = { selectedIconKey = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            ApplicationButton(
                onClick = { onConfirm(name, selectedIconKey, comment.ifBlank { null }) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.categories_confirm))
            }
        }
    }
}

@Composable
private fun EditCategorySheet(
    category: CategoryRowUiModel,
    onConfirm: (name: String, iconKey: String, comment: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var comment by remember { mutableStateOf(category.comment.orEmpty()) }
    var selectedIconKey by remember { mutableStateOf(category.iconKey) }

    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded,
        detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
    )

    AppModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.categories_edit_category),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.categories_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(Res.string.categories_comment_label)) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.categories_icon_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            EmojiPickerGrid(
                selectedIconKey = selectedIconKey,
                onSelectIcon = { selectedIconKey = it },
            )
            Spacer(modifier = Modifier.height(16.dp))
            ApplicationButton(
                onClick = { onConfirm(name, selectedIconKey, comment.ifBlank { null }) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.categories_save))
            }
        }
    }
}

@Composable
private fun AddSubcategorySheet(
    onConfirm: (name: String, comment: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded,
        detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
    )

    AppModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.categories_add_subcategory),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.categories_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(Res.string.categories_comment_label)) },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            ApplicationButton(
                onClick = { onConfirm(name, comment.ifBlank { null }) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.categories_confirm))
            }
        }
    }
}

@Composable
private fun CategoriesDeletionDialogs(
    state: CategoriesState,
    onEvent: (CategoriesEvent) -> Unit,
) {
    when (val dialog = state.deletionDialog) {
        is CategoryDeletionDialog.SimpleConfirm -> SimpleDeleteCategoryDialog(
            onConfirm = { onEvent(CategoriesEvent.ConfirmDeleteCategory(dialog.categoryId)) },
            onDismiss = { onEvent(CategoriesEvent.DismissDeletionDialog) },
        )
        is CategoryDeletionDialog.WithExpenses -> DeleteCategoryWithExpensesDialog(
            dialog = dialog,
            onMoveExpenses = { targetId ->
                onEvent(
                    CategoriesEvent.ConfirmDeleteCategoryMoveExpenses(dialog.categoryId, targetId),
                )
            },
            onDeleteExpenses = {
                onEvent(CategoriesEvent.ConfirmDeleteCategoryDeleteExpenses(dialog.categoryId))
            },
            onDismiss = { onEvent(CategoriesEvent.DismissDeletionDialog) },
        )
        null -> Unit
    }
}

@Composable
private fun DeleteSubcategoryDialog(
    state: CategoriesState,
    onEvent: (CategoriesEvent) -> Unit,
) {
    val subcategory = state.subcategoryToDelete ?: return
    AlertDialog(
        onDismissRequest = { onEvent(CategoriesEvent.DismissDeleteSubcategoryDialog) },
        title = { Text(stringResource(Res.string.categories_delete_subcategory_title)) },
        text = {
            Text(stringResource(Res.string.categories_delete_subcategory_message, subcategory.name))
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(CategoriesEvent.ConfirmDeleteSubcategory(subcategory.id)) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.categories_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(CategoriesEvent.DismissDeleteSubcategoryDialog) }) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    )
}

@Composable
private fun SimpleDeleteCategoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.categories_delete_confirm_title)) },
        text = { Text(stringResource(Res.string.categories_delete_confirm_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.categories_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    )
}

@Composable
private fun DeleteCategoryWithExpensesDialog(
    dialog: CategoryDeletionDialog.WithExpenses,
    onMoveExpenses: (targetCategoryId: Long) -> Unit,
    onDeleteExpenses: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.categories_delete_has_expenses_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        Res.string.categories_delete_has_expenses_message,
                        dialog.expenseCount,
                    ),
                )
                if (dialog.availableTargets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.categories_delete_move_expenses),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(
                            items = dialog.availableTargets,
                            key = { it.id },
                        ) { target ->
                            CategoryTargetRow(
                                target = target,
                                onClick = { onMoveExpenses(target.id) },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDeleteExpenses,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.categories_delete_delete_expenses))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    )
}

@Composable
private fun CategoryTargetRow(
    target: CategoryTargetUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val icon = allCategoryIcons.find { it.key == target.iconKey }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon?.emoji.orEmpty(), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = target.name,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPickerGrid(
    selectedIconKey: String,
    onSelectIcon: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        allCategoryIcons.forEach { icon ->
            val isSelected = icon.key == selectedIconKey
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .clickable { onSelectIcon(icon.key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = icon.emoji, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private val previewCategories = persistentListOf(
    CategoryRowUiModel(
        id = 1L,
        name = "Food",
        iconKey = "ic_food",
        comment = "Groceries & dining",
        subcategories = persistentListOf(
            SubcategoryChipUiModel(id = 10L, name = "Groceries", comment = null),
            SubcategoryChipUiModel(id = 11L, name = "Restaurants", comment = "Eating out"),
        ),
    ),
    CategoryRowUiModel(
        id = 2L,
        name = "Transport",
        iconKey = "ic_transport",
        comment = null,
        subcategories = persistentListOf(),
    ),
)

@Preview(showBackground = true)
@Composable
private fun CategoriesContentPreview() {
    AppTheme {
        CategoriesContent(
            state = CategoriesState.Content(
                categories = previewCategories,
                showAddCategorySheet = false,
                addSubcategoryForCategoryId = null,
                editingCategory = null,
                deletionDialog = null,
            ),
            onEvent = {},
            onGoBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesContentEmptyPreview() {
    AppTheme {
        CategoriesContent(
            state = CategoriesState.Content(
                categories = persistentListOf(),
                showAddCategorySheet = false,
                addSubcategoryForCategoryId = null,
                editingCategory = null,
                deletionDialog = null,
            ),
            onEvent = {},
            onGoBack = {},
        )
    }
}

@Preview
@Composable
private fun EmojiPickerGridPreview() {
    AppTheme {
        EmojiPickerGrid(
            selectedIconKey = "ic_food",
            onSelectIcon = {},
        )
    }
}
