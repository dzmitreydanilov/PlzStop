package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.expenses.create.presentation.CreateExpenseStateHolder
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import com.please.stop.app.features.expenses.edit.presentation.EditExpenseStateHolder
import com.please.stop.app.features.expenses.presentation.AddExpenseEvent
import com.please.stop.app.features.expenses.presentation.AddExpenseNavigation
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.MAX_NOTES_LENGTH
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.MAX_TITLE_LENGTH
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.NOTES_COUNTER_THRESHOLD
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder.Companion.TITLE_COUNTER_THRESHOLD
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.features.expenses.presentation.ConversionState
import com.please.stop.app.features.expenses.presentation.ExpenseFormInput
import com.please.stop.app.features.expenses.presentation.NumericKey
import com.please.stop.app.features.expenses.scanner.rememberDocumentScanner
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.navigation.nav3.HandleNavigationBack
import com.please.stop.app.uicomponents.categoryEmojiForKey
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.sheets.CurrencyPickerSheet
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.date.DatePattern
import com.please.stop.app.utils.date.format
import com.please.stop.app.utils.date.localDateTimeFromMillis
import com.please.stop.app.utils.date.nowMillis
import com.please.stop.app.utils.minorUnitsMultiplier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_analyzing_receipt
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.add_expense_category
import plzstop.composeapp.generated.resources.add_expense_confirm
import plzstop.composeapp.generated.resources.add_expense_delete
import plzstop.composeapp.generated.resources.add_expense_delete_message
import plzstop.composeapp.generated.resources.add_expense_delete_title
import plzstop.composeapp.generated.resources.add_expense_discard
import plzstop.composeapp.generated.resources.add_expense_discard_message
import plzstop.composeapp.generated.resources.add_expense_discard_title
import plzstop.composeapp.generated.resources.add_expense_done
import plzstop.composeapp.generated.resources.add_expense_notes_label
import plzstop.composeapp.generated.resources.add_expense_receipt_no_network
import plzstop.composeapp.generated.resources.add_expense_receipt_not_receipt
import plzstop.composeapp.generated.resources.add_expense_receipt_service_unavailable
import plzstop.composeapp.generated.resources.add_expense_receipt_unreadable
import plzstop.composeapp.generated.resources.add_expense_scan_receipt
import plzstop.composeapp.generated.resources.add_expense_scan_receipt_hint
import plzstop.composeapp.generated.resources.add_expense_select_category
import plzstop.composeapp.generated.resources.add_expense_title
import plzstop.composeapp.generated.resources.add_expense_title_edit
import plzstop.composeapp.generated.resources.add_expense_title_label
import plzstop.composeapp.generated.resources.add_expense_what_was_it_for
import plzstop.composeapp.generated.resources.conversion_converted_amount
import plzstop.composeapp.generated.resources.conversion_custom_rate
import plzstop.composeapp.generated.resources.conversion_fetching_rate
import plzstop.composeapp.generated.resources.conversion_rate_format
import plzstop.composeapp.generated.resources.conversion_rate_override_apply
import plzstop.composeapp.generated.resources.conversion_rate_override_hint
import plzstop.composeapp.generated.resources.conversion_rate_override_title
import plzstop.composeapp.generated.resources.conversion_rate_unavailable
import plzstop.composeapp.generated.resources.conversion_reset_rate
import plzstop.composeapp.generated.resources.conversion_save_in
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_calendar
import plzstop.composeapp.generated.resources.ic_currency_exchange
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_right
import plzstop.composeapp.generated.resources.ic_scan
import plzstop.composeapp.generated.resources.ic_trash_bin
import kotlin.time.ExperimentalTime

private val AMOUNT_DISPLAY_HEIGHT = 64.dp
private val SCAN_LINE_TRAVEL_PX = 40.dp
private const val CATEGORY_GRID_COLUMNS = 3

@Composable
fun CreateExpenseScreen(
    categoryId: Long?,
    onGoBack: () -> Unit,
    onOpenReceiptItems: () -> Unit = {},
) {
    val stateHolder = koinViewModel<CreateExpenseStateHolder>(
        key = "create_expense_$categoryId",
    ) { parametersOf(categoryId) }

    ExpenseScreenContent(
        stateHolder = stateHolder,
        onGoBack = onGoBack,
        onOpenReceiptItems = onOpenReceiptItems,
    )
}

@Composable
fun EditExpenseScreen(
    expenseId: Long,
    onGoBack: () -> Unit,
) {
    val stateHolder = koinViewModel<EditExpenseStateHolder>(
        key = "edit_expense_$expenseId",
    ) { parametersOf(expenseId) }

    ExpenseScreenContent(stateHolder = stateHolder, onGoBack = onGoBack)
}

@Composable
private fun ExpenseScreenContent(
    stateHolder: BaseExpenseStateHolder,
    onGoBack: () -> Unit,
    onOpenReceiptItems: () -> Unit = {},
) {
    val state by stateHolder.state.collectAsStateWithLifecycle()

    HandleNavigationBack(enabled = state.status.hasUnsavedChanges) {
        stateHolder.processEvent(AddExpenseEvent.BackClicked)
    }

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            AddExpenseNavigation.GoBack -> onGoBack()
            AddExpenseNavigation.OpenReceiptItems -> onOpenReceiptItems()
        }
    }

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(AddExpenseEvent.DismissError) },
    ) {

        AddExpenseContent(
            state = state,
            onEvent = stateHolder::processEvent,
        )
    }
}


internal val AddExpenseState.asOverlay: ScreenOverlay?
    @Composable get() = errorOverlay?.let { overlay ->
        ScreenOverlay.Error(
            type = overlay.errorType,
            title = overlay.receiptError?.toOverlayMessage(),
        )
    }

@Composable
private fun ReceiptError.toOverlayMessage(): String = when (this) {
    ReceiptError.NOT_RECEIPT -> stringResource(Res.string.add_expense_receipt_not_receipt)
    ReceiptError.UNREADABLE -> stringResource(Res.string.add_expense_receipt_unreadable)
    ReceiptError.NO_NETWORK -> stringResource(Res.string.add_expense_receipt_no_network)
    ReceiptError.SERVICE_UNAVAILABLE -> stringResource(Res.string.add_expense_receipt_service_unavailable)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun AddExpenseContent(
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

    val selectedCategory = state.selectedCategory
    val tags = state.titleTags

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (editContext.isEditMode) Res.string.add_expense_title_edit
                            else Res.string.add_expense_title
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(AddExpenseEvent.BackClicked) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    if (editContext.isEditMode) {
                        IconButton(onClick = { onEvent(AddExpenseEvent.DeleteClicked) }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_trash_bin),
                                contentDescription = stringResource(Res.string.add_expense_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateTime = remember(form.dateEpochMillis) {
                    localDateTimeFromMillis(form.dateEpochMillis)
                }
                val formattedDate = dateTime.format(DatePattern.EEEE_MMM_DD)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            onEvent(
                                AddExpenseEvent.KeyPressed(NumericKey.Calendar),
                            )
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_calendar),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!editContext.isEditMode) {
                    ScanReceiptCard(
                        isAnalyzing = receipt.isAnalyzing,
                        onClick = {
                            coroutineScope.launch {
                                documentScanner.scan()
                                    .onSuccess { bytes ->
                                        onEvent(AddExpenseEvent.ReceiptScanned(bytes))
                                    }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onEvent(AddExpenseEvent.CreateReceiptClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add receipt manually")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                CategoryChip(
                    selectedCategory = selectedCategory,
                    onClick = { showCategorySheet = true },
                )

                val filteredSubcategories = remember(
                    state.subcategories,
                    form.selectedCategoryId,
                ) {
                    state.subcategories.filter { it.parentCategoryId == form.selectedCategoryId }
                }
                if (filteredSubcategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filteredSubcategories.forEach { sub ->
                            val isSelected = form.selectedSubcategoryId == sub.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onEvent(
                                        AddExpenseEvent.SubcategorySelected(
                                            if (isSelected) null else sub.id,
                                        ),
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${categoryEmojiForKey(sub.iconKey)} ${sub.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (form.title.isNotBlank() || form.notes.isNotBlank()) {
                    NotesSection(
                        title = form.title,
                        notes = form.notes,
                        onClick = { showNotesSheet = true },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        tags.forEach { tag ->
                            val isSelected = form.title == tag
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onEvent(AddExpenseEvent.TitleChanged(if (isSelected) "" else tag))
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

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
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
        ModalBottomSheet(
            onDismissRequest = { showNotesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
        DatePicker(form, onEvent)
    }

    if (status.showDiscardDialog) {
        DiscardDialog(onEvent)
    }

    if (status.showDeleteDialog) {
        DeleteDialog(onEvent)
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
private fun DatePicker(
    form: ExpenseFormInput,
    onEvent: (AddExpenseEvent) -> Unit
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
        onDismissRequest = { },
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
    AlertDialog(
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
    AlertDialog(
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

@Suppress("ModifierHeightWithText")
@Composable
private fun AmountSection(
    state: AddExpenseState,
    form: ExpenseFormInput,
    onCurrencyClick: () -> Unit,
    onEditRate: () -> Unit,
    onResetRate: () -> Unit,
    onToggleSaveInOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = form.amountDisplayExpression.isEmpty()
    val displayText = if (isEmpty) "0 ${state.currency.symbol}" else form.amountDisplayExpression

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isEmpty) 1f else 1.02f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        ),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .height(AMOUNT_DISPLAY_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.displaySmall,
                color = if (isEmpty) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            val isForeignCurrency = state.currencyConversionEnabled &&
                state.currency.code != state.conversion.defaultCurrencyCode &&
                state.currency.code.isNotEmpty() &&
                state.conversion.defaultCurrencyCode.isNotEmpty()
            if (isForeignCurrency) {
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyChip(
                    currencyCode = state.currency.code,
                    conversion = state.conversion,
                    onClick = onCurrencyClick,
                )
            }
        }

        ConversionInfo(
            conversion = state.conversion,
            selectedCurrencyCode = state.currency.code,
            onEditRate = onEditRate,
            onResetRate = onResetRate,
            onToggleSaveInOriginal = onToggleSaveInOriginal,
        )
    }
}

@Composable
private fun CurrencyChip(
    currencyCode: String,
    conversion: ConversionState,
    onClick: () -> Unit,
) {
    val isForeign = currencyCode != conversion.defaultCurrencyCode &&
        currencyCode.isNotEmpty() &&
        conversion.defaultCurrencyCode.isNotEmpty()

    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = currencyCode,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        leadingIcon = if (isForeign) {
            {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_currency_exchange),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(20.dp),
        colors = if (isForeign) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            AssistChipDefaults.assistChipColors()
        },
    )
}

@Composable
private fun ConversionInfo(
    conversion: ConversionState,
    selectedCurrencyCode: String,
    onEditRate: () -> Unit,
    onResetRate: () -> Unit,
    onToggleSaveInOriginal: () -> Unit,
) {
    val isVisible = (conversion.isLoading || conversion.rate != null || conversion.hasFetchError) &&
        selectedCurrencyCode != conversion.defaultCurrencyCode &&
        selectedCurrencyCode.isNotEmpty() &&
        conversion.defaultCurrencyCode.isNotEmpty()

    if (!isVisible) return

    when {
        conversion.isLoading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                )
                Text(
                    text = stringResource(Res.string.conversion_fetching_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        conversion.rate != null -> {
            val rateFormatted = conversion.rate.toString()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(
                        Res.string.conversion_rate_format,
                        selectedCurrencyCode,
                        rateFormatted,
                        conversion.defaultCurrencyCode,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onEditRate),
                )
                if (conversion.isManualOverride) {
                    Text(
                        text = stringResource(Res.string.conversion_custom_rate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    if (conversion.fetchedRate != null) {
                        Text(
                            text = stringResource(Res.string.conversion_reset_rate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onResetRate),
                        )
                    }
                }
            }
            if (conversion.convertedAmountMinorUnits != null) {
                val convertedDisplay = formatMinorUnitsForDisplay(
                    conversion.convertedAmountMinorUnits,
                )
                Text(
                    text = stringResource(
                        Res.string.conversion_converted_amount,
                        convertedDisplay,
                        conversion.defaultCurrencySymbol,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val saveCurrencyCode = if (conversion.saveInOriginalCurrency) {
                selectedCurrencyCode
            } else {
                conversion.defaultCurrencyCode
            }
            Text(
                text = stringResource(Res.string.conversion_save_in, saveCurrencyCode),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onToggleSaveInOriginal),
            )
        }
        else -> {
            Text(
                text = stringResource(Res.string.conversion_rate_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onEditRate),
            )
        }
    }
}

@Composable
private fun ScanReceiptCard(
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

    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = !isAnalyzing,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Scan frame visual
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_scan),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
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
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Animated scan line
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
private fun CategoryChip(
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
private fun NotesSection(
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
private fun CategoryGridSheet(
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
private fun NotesInputSheet(
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

private fun formatMinorUnitsForDisplay(
    minorUnits: Long,
    decimalPlaces: Int = DEFAULT_CURRENCY_DECIMAL_PLACES,
): String {
    val multiplier = minorUnitsMultiplier(decimalPlaces)
    val intPart = minorUnits / multiplier
    val fracPart = (minorUnits % multiplier).toString().padStart(decimalPlaces, '0')
    return "$intPart.$fracPart"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RateOverrideSheet(
    input: String,
    fromCode: String,
    toCode: String,
    onInputChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.conversion_rate_override_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                label = {
                    Text(
                        stringResource(
                            Res.string.conversion_rate_override_hint,
                            fromCode,
                            toCode,
                        ),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onConfirm,
                enabled = input.toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.conversion_rate_override_apply))
            }
        }
    }
}

