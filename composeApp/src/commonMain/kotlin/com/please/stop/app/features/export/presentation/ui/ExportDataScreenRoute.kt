package com.please.stop.app.features.export.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dog.care.utils.uicomponents.modifiers.noRippleClickable
import com.please.stop.app.features.auth.google.GoogleButtonUiContainer
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.presentation.ExportEvent
import com.please.stop.app.features.export.presentation.ExportState
import com.please.stop.app.features.export.presentation.ExportStateHolder
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppDimens
import com.please.stop.app.uicomponents.fields.OutlinedInvertedTextField
import com.please.stop.app.uicomponents.icons.ArrowBackIconButton
import com.please.stop.app.utils.date.localDateTimeFromMillis
import com.please.stop.app.utils.date.nowMillis
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.add_expense_confirm
import plzstop.composeapp.generated.resources.close
import plzstop.composeapp.generated.resources.content_desc_open_date_range_picker
import plzstop.composeapp.generated.resources.export_button
import plzstop.composeapp.generated.resources.export_connect_google_body
import plzstop.composeapp.generated.resources.export_connect_google_button
import plzstop.composeapp.generated.resources.export_connect_google_title
import plzstop.composeapp.generated.resources.export_date_picker_mode_range
import plzstop.composeapp.generated.resources.export_date_picker_mode_single
import plzstop.composeapp.generated.resources.export_date_range_label
import plzstop.composeapp.generated.resources.export_date_range_placeholder
import plzstop.composeapp.generated.resources.export_enable_notifications_body
import plzstop.composeapp.generated.resources.export_enable_notifications_title
import plzstop.composeapp.generated.resources.export_enqueued_message
import plzstop.composeapp.generated.resources.export_failed_body
import plzstop.composeapp.generated.resources.export_failed_title
import plzstop.composeapp.generated.resources.export_no_expenses_body
import plzstop.composeapp.generated.resources.export_no_expenses_title
import plzstop.composeapp.generated.resources.export_organization_method_label
import plzstop.composeapp.generated.resources.export_organization_same_tab
import plzstop.composeapp.generated.resources.export_organization_separate_tabs
import plzstop.composeapp.generated.resources.export_spread_sheet_title
import plzstop.composeapp.generated.resources.export_title
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.month_names_short
import plzstop.composeapp.generated.resources.retry

private const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

@Composable
fun ExportScreenRoute(
    onNavigateBack: () -> Unit,
) {
    val stateHolder = koinViewModel<ExportStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    ExportRouteContent(
        state = state,
        onEvent = stateHolder::processEvent,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportRouteContent(
    state: ExportState,
    onEvent: (ExportEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.export_title)) },
                navigationIcon = {
                    ArrowBackIconButton(onNavigateBack)
                }
            )
        }
    ) { paddingValues ->
        val dimens = LocalAppDimens.current
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.export_spread_sheet_title)) },
                )

                Spacer(Modifier.height(dimens.small2))

                DateRangeField(
                    startDateMillis = state.currentStartDateMillis,
                    endDateMillis = state.currentEndDateMillis,
                    onRangeChange = { start, end ->
                        onEvent(ExportEvent.DateRangeSelected(start, end))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(dimens.small2))

                OrganizationMethodSelector(
                    selected = state.currentSpreadSheetFormat,
                    onSelect = { onEvent(ExportEvent.TabLayoutSelected(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeField(
    startDateMillis: Long,
    endDateMillis: Long,
    onRangeChange: (start: Long, end: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val rangeText = formatDateRange(startDateMillis, endDateMillis)
    val openLabel = stringResource(Res.string.content_desc_open_date_range_picker)

    OutlinedInvertedTextField(
        value = rangeText,
        onValueChange = {},
        modifier = modifier
            .noRippleClickable(onClick = { showPicker = true }, role = Role.Button)
            .semantics { contentDescription = openLabel },
        readOnly = true,
        enabled = false,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
        label = { Text(stringResource(Res.string.export_date_range_label)) },
        placeholder = { Text(stringResource(Res.string.export_date_range_placeholder)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            unfocusedBorderColor = MaterialTheme.colorScheme.primary,
            disabledBorderColor = MaterialTheme.colorScheme.primary,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    )

    if (showPicker) {
        DateSelectionDialog(
            initialStartMillis = startDateMillis,
            initialEndMillis = endDateMillis,
            onConfirm = { start, end ->
                onRangeChange(start, end)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

private enum class DateSelectionMode { SINGLE, RANGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionDialog(
    initialStartMillis: Long,
    initialEndMillis: Long,
    onConfirm: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMode = if (initialStartMillis == initialEndMillis) {
        DateSelectionMode.SINGLE
    } else {
        DateSelectionMode.RANGE
    }
    var mode by remember { mutableStateOf(initialMode) }
    val singleState = rememberDatePickerState(initialSelectedDateMillis = initialStartMillis)
    val rangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis,
    )
    val confirmEnabled = when (mode) {
        DateSelectionMode.SINGLE -> singleState.selectedDateMillis != null
        DateSelectionMode.RANGE ->
            rangeState.selectedStartDateMillis != null &&
                rangeState.selectedEndDateMillis != null
    }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = {
                    when (mode) {
                        DateSelectionMode.SINGLE -> {
                            val day = singleState.selectedDateMillis ?: return@TextButton
                            onConfirm(day, day)
                        }
                        DateSelectionMode.RANGE -> {
                            val start = rangeState.selectedStartDateMillis ?: return@TextButton
                            val end = rangeState.selectedEndDateMillis ?: return@TextButton
                            onConfirm(start, end)
                        }
                    }
                },
            ) { Text(stringResource(Res.string.add_expense_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.add_expense_cancel))
            }
        },
    ) {
        val toggleSlot: @Composable () -> Unit = {
            DatePickerModeToggle(
                mode = mode,
                onModeChange = { mode = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        when (mode) {
            DateSelectionMode.SINGLE -> DatePicker(
                state = singleState,
                title = toggleSlot,
            )
            DateSelectionMode.RANGE -> DateRangePicker(
                state = rangeState,
                title = toggleSlot,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModeToggle(
    mode: DateSelectionMode,
    onModeChange: (DateSelectionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = DateSelectionMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == mode,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(stringResource(option.labelRes())) },
            )
        }
    }
}

private fun DateSelectionMode.labelRes() = when (this) {
    DateSelectionMode.SINGLE -> Res.string.export_date_picker_mode_single
    DateSelectionMode.RANGE -> Res.string.export_date_picker_mode_range
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizationMethodSelector(
    selected: SpreadSheetFormat,
    onSelect: (SpreadSheetFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.export_organization_method_label).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimens.extraSmall))
        val options = SpreadSheetFormat.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, format ->
                SegmentedButton(
                    selected = format == selected,
                    onClick = { onSelect(format) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(stringResource(format.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun formatDateRange(startMillis: Long, endMillis: Long): String {
    val months = stringArrayResource(Res.array.month_names_short)
    val startText = remember(startMillis, months) { formatShortDate(startMillis, months) }
    if (startMillis == endMillis) return startText
    val endText = remember(endMillis, months) { formatShortDate(endMillis, months) }
    return "$startText – $endText"
}

private fun formatShortDate(millis: Long, monthNamesShort: List<String>): String {
    val date = localDateTimeFromMillis(millis).date
    val monthName = monthNamesShort[date.month.number - 1]
    return "${date.day} $monthName ${date.year}"
}

private fun SpreadSheetFormat.labelRes() = when (this) {
    SpreadSheetFormat.SINGLE_TAB -> Res.string.export_organization_same_tab
    SpreadSheetFormat.SEPARATE_TABS -> Res.string.export_organization_separate_tabs
}

@Composable
private fun EnqueuedContent() {
    Icon(
        imageVector = vectorResource(Res.drawable.ic_check),
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(Res.string.export_enqueued_message),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun NeedsGoogleContent(onEvent: (ExportEvent) -> Unit) {
    Text(
        stringResource(Res.string.export_connect_google_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.export_connect_google_body),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(16.dp))
    GoogleButtonUiContainer(
        modifier = Modifier.fillMaxWidth(),
        filterByAuthorizedAccounts = false,
        onGoogleSignInResult = { user ->
            if (user != null) onEvent(ExportEvent.GoogleAccountConnected(user))
        },
    ) {
        Button(onClick = ::onClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.export_connect_google_button))
        }
    }
}

@Composable
private fun NeedsNotificationContent(onDismiss: () -> Unit) {
    Text(
        stringResource(Res.string.export_enable_notifications_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.export_enable_notifications_body),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.close))
    }
}

@Composable
private fun NoExpensesContent(onDismiss: () -> Unit) {
    Text(
        stringResource(Res.string.export_no_expenses_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.export_no_expenses_body),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.close))
    }
}

@Composable
private fun ErrorContent(onEvent: (ExportEvent) -> Unit) {
    Text(
        stringResource(Res.string.export_failed_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.export_failed_body),
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onEvent(ExportEvent.DismissError) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.retry))
    }
}

@Suppress("UnusedPrivateMember")
@Composable
private fun ConfirmContent(state: ExportState, onEvent: (ExportEvent) -> Unit) {
    Text(stringResource(Res.string.export_title), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    GoogleButtonUiContainer(
        modifier = Modifier.fillMaxWidth(),
        filterByAuthorizedAccounts = false,
        scopes = listOf(SHEETS_SCOPE, DRIVE_FILE_SCOPE),
        onGoogleSignInResult = { user ->
            if (user?.accessToken != null) {
                onEvent(ExportEvent.ConfirmExport(user.accessToken))
            }
        },
    ) {
        Button(onClick = ::onClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.export_button))
        }
    }
}

@Preview
@Composable
private fun ExportRouteContentPreview() {
    AppTheme {
        ExportRouteContent(
            state = ExportState.Confirm(
                currentSpreadSheetFormat = SpreadSheetFormat.SINGLE_TAB,
                currentStartDateMillis = nowMillis(),
                currentEndDateMillis = nowMillis(),
            ),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}
