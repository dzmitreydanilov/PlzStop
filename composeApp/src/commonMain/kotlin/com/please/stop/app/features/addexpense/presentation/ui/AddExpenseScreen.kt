package com.please.stop.app.features.addexpense.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.addexpense.presentation.AddExpenseEvent
import com.please.stop.app.features.addexpense.presentation.AddExpenseNavigation
import com.please.stop.app.features.addexpense.presentation.AddExpenseState
import com.please.stop.app.features.addexpense.presentation.AddExpenseStateHolder
import com.please.stop.app.features.addexpense.domain.model.ReceiptError
import com.please.stop.app.features.addexpense.scanner.DocumentScanner
import com.please.stop.app.features.addexpense.presentation.AddExpenseStateHolder.Companion.MAX_NOTES_LENGTH
import com.please.stop.app.features.addexpense.presentation.AddExpenseStateHolder.Companion.MAX_TITLE_LENGTH
import com.please.stop.app.features.addexpense.presentation.AddExpenseStateHolder.Companion.NOTES_COUNTER_THRESHOLD
import com.please.stop.app.features.addexpense.presentation.AddExpenseStateHolder.Companion.TITLE_COUNTER_THRESHOLD
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_add
import plzstop.composeapp.generated.resources.add_expense_analyzing_receipt
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.add_expense_category
import plzstop.composeapp.generated.resources.add_expense_confirm
import plzstop.composeapp.generated.resources.add_expense_date
import plzstop.composeapp.generated.resources.add_expense_delete
import plzstop.composeapp.generated.resources.add_expense_delete_message
import plzstop.composeapp.generated.resources.add_expense_delete_title
import plzstop.composeapp.generated.resources.add_expense_discard
import plzstop.composeapp.generated.resources.add_expense_discard_message
import plzstop.composeapp.generated.resources.add_expense_discard_title
import plzstop.composeapp.generated.resources.add_expense_notes
import plzstop.composeapp.generated.resources.add_expense_receipt_no_network
import plzstop.composeapp.generated.resources.add_expense_receipt_service_unavailable
import plzstop.composeapp.generated.resources.add_expense_receipt_unreadable
import plzstop.composeapp.generated.resources.add_expense_save_changes
import plzstop.composeapp.generated.resources.add_expense_scan_receipt
import plzstop.composeapp.generated.resources.add_expense_time
import plzstop.composeapp.generated.resources.add_expense_title
import plzstop.composeapp.generated.resources.add_expense_title_edit
import plzstop.composeapp.generated.resources.add_expense_title_label
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_photo_camera
import plzstop.composeapp.generated.resources.ic_trash_bin
import plzstop.composeapp.generated.resources.onboarding_something_went_wrong

@Composable
fun AddExpenseScreen(
    expenseId: Long?,
    preselectedCategoryId: Long? = null,
    onGoBack: () -> Unit,
) {
    val stateHolder = koinViewModel<AddExpenseStateHolder>(
        key = "add_expense_${expenseId ?: "new"}_${preselectedCategoryId ?: "none"}",
    ) { parametersOf(expenseId, preselectedCategoryId) }
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { nav ->
        when (nav) {
            AddExpenseNavigation.GoBack -> onGoBack()
        }
    }

    when (val s = state) {
        is AddExpenseState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is AddExpenseState.Content -> {
            ScreenOverlayContainer(
                overlay = asOverlay(state = s),
                onDismiss = { stateHolder.processEvent(AddExpenseEvent.DismissError) },
            ) {
                AddExpenseContent(
                    state = s,
                    onEvent = stateHolder::processEvent,
                )
            }
        }

        is AddExpenseState.Error -> {
            ScreenOverlayContainer(
                overlay = ScreenOverlay.Error(type = s.errorType),
                onDismiss = onGoBack,
                onRetry = onGoBack,
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun asOverlay(state: AddExpenseState.Content): ScreenOverlay? {
    return when {
        state.errorType != null -> ScreenOverlay.Error(type = state.errorType)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun AddExpenseContent(
    state: AddExpenseState.Content,
    onEvent: (AddExpenseEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val documentScanner = koinInject<DocumentScanner>()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val appColors = com.please.stop.app.theme.LocalAppColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditMode) Res.string.add_expense_title_edit
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
                    if (state.isEditMode) {
                        IconButton(onClick = { onEvent(AddExpenseEvent.DeleteClicked) }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_trash_bin),
                                contentDescription = stringResource(Res.string.add_expense_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(
                        onClick = { onEvent(AddExpenseEvent.SaveClicked) },
                        enabled = state.isFormValid && !state.isSaving,
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    if (state.isEditMode) Res.string.add_expense_save_changes
                                    else Res.string.add_expense_add
                                ),
                                color = appColors.teal600,
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
                    .verticalScroll(rememberScrollState()),
            ) {
                AmountDisplay(
                    amountInput = state.amountInput,
                    currencySymbol = state.currencySymbol,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                )

                if (!state.isEditMode) {
                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                documentScanner.scan()
                                    .onSuccess { bytes ->
                                        onEvent(AddExpenseEvent.ReceiptScanned(bytes))
                                    }
                            }
                        },
                        enabled = !state.isAnalyzingReceipt,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_photo_camera),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(Res.string.add_expense_scan_receipt))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onEvent(AddExpenseEvent.TitleChanged(it)) },
                    label = { Text(stringResource(Res.string.add_expense_title_label)) },
                    singleLine = true,
                    supportingText = if (state.title.length > TITLE_COUNTER_THRESHOLD) {
                        { Text("${state.title.length}/$MAX_TITLE_LENGTH") }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.add_expense_category),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CategoryPicker(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelected = { onEvent(AddExpenseEvent.CategorySelected(it)) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                val dateTime = remember(state.dateEpochMillis) {
                    Instant.fromEpochMilliseconds(state.dateEpochMillis)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                }

                OutlinedTextField(
                    value = "${dateTime.day}/${dateTime.month}/${dateTime.year}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.add_expense_date)) },
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showDatePicker = true },
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.add_expense_time)) },
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showTimePicker = true },
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onEvent(AddExpenseEvent.NotesChanged(it)) },
                    label = { Text(stringResource(Res.string.add_expense_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    supportingText = if (state.notes.length > NOTES_COUNTER_THRESHOLD) {
                        { Text("${state.notes.length}/$MAX_NOTES_LENGTH") }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            NumericKeyboard(
                showDecimal = state.decimalPlaces > 0,
                onKey = { onEvent(AddExpenseEvent.KeyPressed(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }

    if (showDatePicker) {
        val nowMillis = remember { Clock.System.now().toEpochMilliseconds() }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateEpochMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= nowMillis
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onEvent(AddExpenseEvent.DateChanged(it))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(Res.string.add_expense_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.add_expense_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val dateTime = remember(state.dateEpochMillis) {
            Instant.fromEpochMilliseconds(state.dateEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(
                            AddExpenseEvent.TimeChanged(
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                            )
                        )
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(Res.string.add_expense_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(Res.string.add_expense_cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }

    if (state.showDiscardDialog) {
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

    if (state.showDeleteDialog) {
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

    if (state.isAnalyzingReceipt) {
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

    if (state.receiptError != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { onEvent(AddExpenseEvent.DismissReceiptError) }) {
                        Text(stringResource(Res.string.add_expense_confirm))
                    }
                },
            ) {
                Text(text = state.receiptError.toMessage())
            }
        }
    }
}

@Composable
private fun ReceiptError.toMessage(): String = when (this) {
    ReceiptError.UNREADABLE -> stringResource(Res.string.add_expense_receipt_unreadable)
    ReceiptError.NO_NETWORK -> stringResource(Res.string.add_expense_receipt_no_network)
    ReceiptError.SERVICE_UNAVAILABLE -> stringResource(Res.string.add_expense_receipt_service_unavailable)
}

@Composable
private fun AmountDisplay(
    amountInput: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    val appColors = com.please.stop.app.theme.LocalAppColors.current
    val displayText = if (amountInput.isEmpty()) {
        "${currencySymbol}0"
    } else {
        "$currencySymbol$amountInput"
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (amountInput.isEmpty()) 1f else 1.02f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        ),
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            )
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.displayLarge,
            color = if (amountInput.isEmpty()) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            } else {
                appColors.teal600
            },
            textAlign = TextAlign.Center,
        )
    }
}
