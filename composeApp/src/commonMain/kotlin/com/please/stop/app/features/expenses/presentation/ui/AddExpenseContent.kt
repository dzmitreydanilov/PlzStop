package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.ModalBottomSheetProperties
import com.please.stop.app.features.expenses.presentation.AddExpenseEvent
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.features.expenses.presentation.ExpenseFormInput
import com.please.stop.app.features.expenses.presentation.NumericKey
import com.please.stop.app.features.expenses.presentation.SubcategoryUiModel
import com.please.stop.app.features.expenses.scanner.rememberDocumentScanner
import com.please.stop.app.uicomponents.CategoryIconImage
import com.please.stop.app.uicomponents.sheets.AnimatedAppModalBottomSheet
import com.please.stop.app.uicomponents.sheets.currency.CurrencyPickerSheet
import com.please.stop.app.utils.date.DatePattern
import com.please.stop.app.utils.date.format
import com.please.stop.app.utils.date.localDateTimeFromMillis
import com.please.stop.app.utils.date.nowMillis
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_add_receipt_manually
import plzstop.composeapp.generated.resources.add_expense_add_subcategory
import plzstop.composeapp.generated.resources.add_expense_all_subcategories
import plzstop.composeapp.generated.resources.add_expense_analyzing_receipt
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.add_expense_change_subcategory
import plzstop.composeapp.generated.resources.add_expense_confirm
import plzstop.composeapp.generated.resources.add_expense_create_subcategory
import plzstop.composeapp.generated.resources.add_expense_delete
import plzstop.composeapp.generated.resources.add_expense_delete_message
import plzstop.composeapp.generated.resources.add_expense_delete_title
import plzstop.composeapp.generated.resources.add_expense_discard
import plzstop.composeapp.generated.resources.add_expense_discard_message
import plzstop.composeapp.generated.resources.add_expense_discard_title
import plzstop.composeapp.generated.resources.add_expense_frequent_subcategories
import plzstop.composeapp.generated.resources.add_expense_no_subcategory_results
import plzstop.composeapp.generated.resources.add_expense_scan_receipt
import plzstop.composeapp.generated.resources.add_expense_search_subcategories
import plzstop.composeapp.generated.resources.add_expense_select_subcategory
import plzstop.composeapp.generated.resources.add_expense_title
import plzstop.composeapp.generated.resources.add_expense_title_edit
import plzstop.composeapp.generated.resources.add_expense_under_category
import plzstop.composeapp.generated.resources.content_desc_clear
import plzstop.composeapp.generated.resources.content_desc_close
import plzstop.composeapp.generated.resources.content_desc_navigate_back
import plzstop.composeapp.generated.resources.ic_add
import plzstop.composeapp.generated.resources.ic_add_note
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_calendar
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.ic_close
import plzstop.composeapp.generated.resources.ic_edit
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_down
import plzstop.composeapp.generated.resources.ic_scan
import plzstop.composeapp.generated.resources.ic_search
import plzstop.composeapp.generated.resources.ic_trash_bin
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
internal fun AddExpenseContent(
    state: AddExpenseState,
    onEvent: (AddExpenseEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val documentScanner = rememberDocumentScanner()
    var showSubcategorySheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    val onScanReceipt: () -> Unit = {
        coroutineScope.launch {
            documentScanner.scan()
                .onSuccess { bytes ->
                    onEvent(AddExpenseEvent.ReceiptScanned(bytes))
                }
        }
    }

    Scaffold(
        topBar = {
            AddExpenseTopBar(
                isEditMode = state.editContext.isEditMode,
                isReceiptAnalyzing = state.receipt.isAnalyzing,
                onNavigateBack = { onEvent(AddExpenseEvent.BackClicked) },
                onCreateReceiptManually = { onEvent(AddExpenseEvent.CreateReceiptClicked) },
                onScanReceipt = onScanReceipt,
                onDeleteClick = { onEvent(AddExpenseEvent.DeleteClicked) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AmountSection(
                state = state,
                form = state.form,
                onCurrencyClick = { onEvent(AddExpenseEvent.KeyPressed(NumericKey.CurrencySymbol)) },
                onEditRate = { onEvent(AddExpenseEvent.ShowRateOverrideSheet) },
                onResetRate = { onEvent(AddExpenseEvent.ResetToFetchedRate) },
                onToggleSaveInOriginal = { onEvent(AddExpenseEvent.ToggleSaveInOriginalCurrency) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            DateHeader(
                dateEpochMillis = state.form.dateEpochMillis,
                onClick = { onEvent(AddExpenseEvent.KeyPressed(NumericKey.Calendar)) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp),
            )

            ExpenseFormSection(
                state = state,
                subcategories = state.filteredSubcategories,
                onSelectCategory = { categoryId -> onEvent(AddExpenseEvent.CategorySelected(categoryId)) },
                onOpenSubcategorySheet = { showSubcategorySheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            NumericKeyboard(
                currencySymbol = state.currency.symbol,
                isInExpressionMode = state.form.isInExpressionMode,
                isSaving = state.status.isSaving,
                isSaveEnabled = state.status.isFormValid,
                onKey = { key ->
                    if (key is NumericKey.Notes) {
                        showNotesSheet = true
                    } else {
                        onEvent(AddExpenseEvent.KeyPressed(key))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }

    if (showSubcategorySheet) {
        AnimatedAppModalBottomSheet(
            onDismiss = { showSubcategorySheet = false },
            showDragIndicator = false,
            properties = ModalBottomSheetProperties(offsetForIme = true),
        ) { dismiss ->
            SubcategoryPickerSheet(
                categoryName = state.selectedCategory?.name.orEmpty(),
                subcategories = state.filteredSubcategories,
                frequentSubcategories = state.frequentSubcategories,
                selectedSubcategoryId = state.form.selectedSubcategoryId,
                onDismiss = dismiss,
                onSelectSubcategory = { subcategoryId ->
                    onEvent(AddExpenseEvent.SubcategorySelected(subcategoryId))
                    dismiss()
                },
                onCreateSubcategory = { name ->
                    onEvent(AddExpenseEvent.CreateSubcategory(name))
                    dismiss()
                },
            )
        }
    }
    if (showNotesSheet) {
        AnimatedAppModalBottomSheet(
            onDismiss = { showNotesSheet = false },
        ) { dismiss ->
            NotesInputSheet(
                title = state.form.title,
                notes = state.form.notes,
                onChangeTitle = { onEvent(AddExpenseEvent.TitleChanged(it)) },
                onChangeNotes = { onEvent(AddExpenseEvent.NotesChanged(it)) },
                onDone = dismiss,
            )
        }
    }

    if (state.status.showDatePicker) {
        AddExpenseDatePicker(form = state.form, onEvent = onEvent)
    }

    if (state.status.showDiscardDialog) {
        DiscardDialog(onEvent = onEvent)
    }

    if (state.status.showDeleteDialog) {
        DeleteDialog(onEvent = onEvent)
    }

    if (state.receipt.isAnalyzing) {
        AnalyzingInProgress()
    }

    if (state.showCurrencyPicker) {
        CurrencyPickerSheet(
            selectedCurrencyCode = state.currency.code,
            deviceCurrencyCode = null,
            onCurrencySelect = { onEvent(AddExpenseEvent.ExpenseCurrencySelected(it)) },
            onDismiss = { onEvent(AddExpenseEvent.DismissCurrencyPicker) },
        )
    }

    if (state.conversion.showRateEditSheet) {
        RateOverrideSheet(
            input = state.conversion.rateEditInput,
            fromCode = state.currency.code,
            toCode = state.conversion.defaultCurrencyCode,
            onInputChange = { onEvent(AddExpenseEvent.RateOverrideInputChanged(it)) },
            onConfirm = { onEvent(AddExpenseEvent.ConfirmRateOverride) },
            onDismiss = { onEvent(AddExpenseEvent.DismissRateOverrideSheet) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseTopBar(
    isEditMode: Boolean,
    isReceiptAnalyzing: Boolean,
    onNavigateBack: () -> Unit,
    onCreateReceiptManually: () -> Unit,
    onScanReceipt: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(
                    if (isEditMode) Res.string.add_expense_title_edit
                    else Res.string.add_expense_title,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.content_desc_navigate_back),
                )
            }
        },
        actions = {
            if (isEditMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_trash_bin),
                        contentDescription = stringResource(Res.string.add_expense_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                IconButton(onClick = onCreateReceiptManually) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_add_note),
                        contentDescription = stringResource(
                            Res.string.add_expense_add_receipt_manually,
                        ),
                    )
                }
                IconButton(
                    onClick = onScanReceipt,
                    enabled = !isReceiptAnalyzing,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_scan),
                        contentDescription = stringResource(Res.string.add_expense_scan_receipt),
                    )
                }
            }
        },
    )
}

@Composable
private fun ExpenseFormSection(
    state: AddExpenseState,
    subcategories: ImmutableList<SubcategoryUiModel>,
    onSelectCategory: (Long) -> Unit,
    onOpenSubcategorySheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val selectedSubcategory = subcategories.firstOrNull { it.id == form.selectedSubcategoryId }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        CategoryLazyRow(
            categories = state.categories,
            selectedCategoryId = form.selectedCategoryId,
            selectedSubcategory = selectedSubcategory,
            onSelectCategory = onSelectCategory,
        )

        if (subcategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SubcategoryAction(
                hasSelectedSubcategory = selectedSubcategory != null,
                onClick = onOpenSubcategorySheet,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CategoryLazyRow(
    categories: ImmutableList<CategoryUiModel>,
    selectedCategoryId: Long?,
    selectedSubcategory: SubcategoryUiModel?,
    onSelectCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryCircleItem(
                category = category,
                isSelected = category.id == selectedCategoryId,
                selectedSubcategory = selectedSubcategory,
                onClick = {
                    onSelectCategory(category.id)
                },
            )
        }
    }
}

@Composable
private fun CategoryCircleItem(
    category: CategoryUiModel,
    isSelected: Boolean,
    selectedSubcategory: SubcategoryUiModel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
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
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = if (isSelected && selectedSubcategory != null) selectedSubcategory.name else "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SubcategoryAction(
    hasSelectedSubcategory: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = vectorResource(
                if (hasSelectedSubcategory) Res.drawable.ic_edit
                else Res.drawable.ic_add,
            ),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(
                if (hasSelectedSubcategory) Res.string.add_expense_change_subcategory
                else Res.string.add_expense_add_subcategory,
            ),
        )
    }
}

@Composable
private fun DateHeader(
    dateEpochMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = localDateTimeFromMillis(dateEpochMillis).format(DatePattern.EEEE_MMM_DD)

    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_calendar),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubcategoryPickerSheet(
    categoryName: String,
    subcategories: ImmutableList<SubcategoryUiModel>,
    frequentSubcategories: ImmutableList<SubcategoryUiModel>,
    selectedSubcategoryId: Long?,
    onDismiss: () -> Unit,
    onSelectSubcategory: (Long?) -> Unit,
    onCreateSubcategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val trimmedQuery = searchQuery.trim()
    val filteredSubcategories = remember(subcategories, trimmedQuery) {
        if (trimmedQuery.isBlank()) {
            subcategories
        } else {
            subcategories.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
        }
    }
    val frequentIds = remember(frequentSubcategories) {
        frequentSubcategories.map { it.id }.toSet()
    }
    val allSectionSubcategories = remember(filteredSubcategories, frequentIds, trimmedQuery) {
        if (trimmedQuery.isBlank()) {
            filteredSubcategories.filterNot { it.id in frequentIds }
        } else {
            filteredSubcategories
        }
    }
    val canCreateSubcategory = trimmedQuery.isNotBlank() &&
        subcategories.none { it.name.equals(trimmedQuery, ignoreCase = true) }

    Column(
        modifier = modifier
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.add_expense_select_subcategory),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(Res.string.add_expense_under_category, categoryName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_close),
                    contentDescription = stringResource(Res.string.content_desc_close),
                )
            }
        }
        HorizontalDivider()
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.add_expense_search_subcategories)) },
            leadingIcon = {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_search),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.content_desc_clear),
                        )
                    }
                }
            },
        )
        if (trimmedQuery.isBlank() && frequentSubcategories.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.add_expense_frequent_subcategories),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                frequentSubcategories.forEach { subcategory ->
                    val isSelected = selectedSubcategoryId == subcategory.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectSubcategory(if (isSelected) null else subcategory.id)
                        },
                        leadingIcon = {
                            CategoryIconImage(
                                iconKey = subcategory.iconKey,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = {
                            Text(
                                text = subcategory.name,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }
        }
        if (allSectionSubcategories.isNotEmpty() || trimmedQuery.isNotBlank() || canCreateSubcategory) {
            Text(
                text = stringResource(Res.string.add_expense_all_subcategories, categoryName),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                items(
                    items = allSectionSubcategories,
                    key = { it.id },
                ) { subcategory ->
                    SubcategoryPickerRow(
                        subcategory = subcategory,
                        isSelected = selectedSubcategoryId == subcategory.id,
                        onClick = {
                            val selectedId = if (selectedSubcategoryId == subcategory.id) null else subcategory.id
                            onSelectSubcategory(selectedId)
                        },
                    )
                }
                if (trimmedQuery.isNotBlank() && allSectionSubcategories.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(Res.string.add_expense_no_subcategory_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
                if (canCreateSubcategory) {
                    item(key = "create") {
                        SubcategoryCreateRow(
                            name = trimmedQuery,
                            onClick = { onCreateSubcategory(trimmedQuery) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryPickerRow(
    subcategory: SubcategoryUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconImage(
            iconKey = subcategory.iconKey,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )
        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SubcategoryCreateRow(
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_add),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(32.dp),
        )
        Text(
            text = stringResource(Res.string.add_expense_create_subcategory, name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDatePicker(
    form: ExpenseFormInput,
    onEvent: (AddExpenseEvent) -> Unit,
) {
    val nowMillis = remember { nowMillis() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = form.dateEpochMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= nowMillis
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onEvent(AddExpenseEvent.DateChanged(it))
                    }
                },
            ) {
                Text(stringResource(Res.string.add_expense_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(AddExpenseEvent.DismissDatePicker) }) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun DiscardDialog(onEvent: (AddExpenseEvent) -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onEvent(AddExpenseEvent.DismissDiscardDialog) },
        title = { Text(stringResource(Res.string.add_expense_discard_title)) },
        text = { Text(stringResource(Res.string.add_expense_discard_message)) },
        confirmButton = {
            TextButton(onClick = { onEvent(AddExpenseEvent.ConfirmDiscard) }) {
                Text(stringResource(Res.string.add_expense_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(AddExpenseEvent.DismissDiscardDialog) }) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    )
}

@Composable
private fun DeleteDialog(onEvent: (AddExpenseEvent) -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onEvent(AddExpenseEvent.DismissDeleteDialog) },
        title = { Text(stringResource(Res.string.add_expense_delete_title)) },
        text = { Text(stringResource(Res.string.add_expense_delete_message)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(AddExpenseEvent.ConfirmDelete) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.add_expense_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(AddExpenseEvent.DismissDeleteDialog) }) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    )
}

@Composable
private fun AnalyzingInProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.add_expense_analyzing_receipt),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
