package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composeunstyled.SheetDetent
import com.composeunstyled.rememberModalBottomSheetState
import com.please.stop.app.features.expenses.presentation.AddExpenseEvent
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.ExpenseFormInput
import com.please.stop.app.features.expenses.presentation.NumericKey
import com.please.stop.app.features.expenses.presentation.SubcategoryUiModel
import com.please.stop.app.features.expenses.scanner.rememberDocumentScanner
import com.please.stop.app.uicomponents.categoryEmojiForKey
import com.please.stop.app.uicomponents.sheets.AppModalBottomSheet
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
import plzstop.composeapp.generated.resources.add_expense_analyzing_receipt
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.add_expense_confirm
import plzstop.composeapp.generated.resources.add_expense_delete
import plzstop.composeapp.generated.resources.add_expense_delete_message
import plzstop.composeapp.generated.resources.add_expense_delete_title
import plzstop.composeapp.generated.resources.add_expense_discard
import plzstop.composeapp.generated.resources.add_expense_discard_message
import plzstop.composeapp.generated.resources.add_expense_discard_title
import plzstop.composeapp.generated.resources.add_expense_title
import plzstop.composeapp.generated.resources.add_expense_title_edit
import plzstop.composeapp.generated.resources.content_desc_navigate_back
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_calendar
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
    var showCategorySheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }

    val form = state.form
    val editContext = state.editContext
    val status = state.status
    val receipt = state.receipt

    val filteredSubcategories = remember(
        state.subcategories,
        form.selectedCategoryId,
    ) {
        state.subcategories.filter { it.parentCategoryId == form.selectedCategoryId }
    }

    Scaffold(
        topBar = {
            AddExpenseTopBar(
                isEditMode = editContext.isEditMode,
                onNavigateBack = { onEvent(AddExpenseEvent.BackClicked) },
                onDeleteClick = { onEvent(AddExpenseEvent.DeleteClicked) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            ExpenseFormSection(
                state = state,
                filteredSubcategories = filteredSubcategories,
                onOpenDatePicker = { onEvent(AddExpenseEvent.KeyPressed(NumericKey.Calendar)) },
                onScanReceipt = {
                    coroutineScope.launch {
                        documentScanner.scan()
                            .onSuccess { bytes ->
                                onEvent(AddExpenseEvent.ReceiptScanned(bytes))
                            }
                    }
                },
                onCreateReceiptManually = { onEvent(AddExpenseEvent.CreateReceiptClicked) },
                onOpenCategorySheet = { showCategorySheet = true },
                onSelectSubcategory = { subcategoryId ->
                    onEvent(AddExpenseEvent.SubcategorySelected(subcategoryId))
                },
                onOpenNotesSheet = { showNotesSheet = true },
                onSelectTitleTag = { title -> onEvent(AddExpenseEvent.TitleChanged(title)) },
                modifier = Modifier.weight(1f),
            )

            AmountSection(
                state = state,
                form = form,
                onCurrencyClick = { onEvent(AddExpenseEvent.KeyPressed(NumericKey.CurrencySymbol)) },
                onEditRate = { onEvent(AddExpenseEvent.ShowRateOverrideSheet) },
                onResetRate = { onEvent(AddExpenseEvent.ResetToFetchedRate) },
                onToggleSaveInOriginal = { onEvent(AddExpenseEvent.ToggleSaveInOriginalCurrency) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            NumericKeyboard(
                currencySymbol = state.currency.symbol,
                isInExpressionMode = form.isInExpressionMode,
                isSaving = status.isSaving,
                isSaveEnabled = status.isFormValid,
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

    if (showCategorySheet) {
        val sheetState = rememberModalBottomSheetState(
            initialDetent = SheetDetent.FullyExpanded,
            detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
        )
        AppModalBottomSheet(
            state = sheetState,
            onDismiss = { showCategorySheet = false },
        ) {
            CategoryGridSheet(
                categories = state.categories,
                selectedCategoryId = form.selectedCategoryId,
                onSelectCategory = { id ->
                    onEvent(AddExpenseEvent.CategorySelected(id))
                    showCategorySheet = false
                },
            )
        }
    }
    if (showNotesSheet) {
        val sheetState = rememberModalBottomSheetState(
            initialDetent = SheetDetent.FullyExpanded,
            detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
        )
        AppModalBottomSheet(
            state = sheetState,
            onDismiss = { showNotesSheet = false },
        ) {
            NotesInputSheet(
                title = form.title,
                notes = form.notes,
                onChangeTitle = { onEvent(AddExpenseEvent.TitleChanged(it)) },
                onChangeNotes = { onEvent(AddExpenseEvent.NotesChanged(it)) },
                onDone = { showNotesSheet = false },
            )
        }
    }

    if (status.showDatePicker) {
        AddExpenseDatePicker(form = form, onEvent = onEvent)
    }

    if (status.showDiscardDialog) {
        DiscardDialog(onEvent = onEvent)
    }

    if (status.showDeleteDialog) {
        DeleteDialog(onEvent = onEvent)
    }

    if (receipt.isAnalyzing) {
        AnalyzingInProgress()
    }

    if (state.showCurrencyPicker) {
        CurrencyPickerSheet(
            selectedCurrencyCode = state.currency.code,
            deviceCurrencyCode = null,
            onCurrencySelected = { onEvent(AddExpenseEvent.ExpenseCurrencySelected(it)) },
            onDismiss = { onEvent(AddExpenseEvent.DismissCurrencyPicker) },
        )
    }

    if (state.conversion.showRateEditSheet) {
        RateOverrideSheet(
            input = state.conversion.rateEditInput,
            fromCode = state.currency.code,
            toCode = state.conversion.defaultCurrencyCode,
            onInputChanged = { onEvent(AddExpenseEvent.RateOverrideInputChanged(it)) },
            onConfirm = { onEvent(AddExpenseEvent.ConfirmRateOverride) },
            onDismiss = { onEvent(AddExpenseEvent.DismissRateOverrideSheet) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseTopBar(
    isEditMode: Boolean,
    onNavigateBack: () -> Unit,
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
            }
        },
    )
}

@Composable
private fun ExpenseFormSection(
    state: AddExpenseState,
    filteredSubcategories: List<SubcategoryUiModel>,
    onOpenDatePicker: () -> Unit,
    onScanReceipt: () -> Unit,
    onCreateReceiptManually: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onSelectSubcategory: (Long?) -> Unit,
    onOpenNotesSheet: () -> Unit,
    onSelectTitleTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    val editContext = state.editContext

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        DateHeader(
            dateEpochMillis = form.dateEpochMillis,
            onClick = onOpenDatePicker,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!editContext.isEditMode) {
            ScanReceiptCard(
                isAnalyzing = state.receipt.isAnalyzing,
                onClick = onScanReceipt,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onCreateReceiptManually,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.add_expense_add_receipt_manually))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        CategoryChip(
            selectedCategory = state.selectedCategory,
            onClick = onOpenCategorySheet,
        )

        if (filteredSubcategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SubcategoryChipsRow(
                subcategories = filteredSubcategories,
                selectedSubcategoryId = form.selectedSubcategoryId,
                onSelectSubcategory = onSelectSubcategory,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (form.title.isNotBlank() || form.notes.isNotBlank()) {
            NotesSection(
                title = form.title,
                notes = form.notes,
                onClick = onOpenNotesSheet,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (state.titleTags.isNotEmpty()) {
            TitleTagRow(
                tags = state.titleTags,
                selectedTitle = form.title,
                onSelectTitleTag = onSelectTitleTag,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DateHeader(
    dateEpochMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = localDateTimeFromMillis(dateEpochMillis).format(DatePattern.EEEE_MMM_DD)

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleIcon(
            icon = vectorResource(Res.drawable.ic_calendar),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubcategoryChipsRow(
    subcategories: List<SubcategoryUiModel>,
    selectedSubcategoryId: Long?,
    onSelectSubcategory: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        subcategories.forEach { subcategory ->
            val isSelected = selectedSubcategoryId == subcategory.id
            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectSubcategory(if (isSelected) null else subcategory.id)
                },
                label = {
                    Text(
                        text = "${categoryEmojiForKey(subcategory.iconKey)} ${subcategory.name}",
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

@Composable
private fun TitleTagRow(
    tags: ImmutableList<String>,
    selectedTitle: String,
    onSelectTitleTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val isSelected = selectedTitle == tag
            FilterChip(
                selected = isSelected,
                onClick = { onSelectTitleTag(if (isSelected) "" else tag) },
                label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
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
    androidx.compose.foundation.layout.Box(
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

@Composable
private fun SimpleIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
