package com.please.stop.app.features.export.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.dog.care.utils.uicomponents.modifiers.noRippleClickable
import com.please.stop.app.uicomponents.fields.OutlinedInvertedTextField
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_cancel
import plzstop.composeapp.generated.resources.add_expense_confirm
import plzstop.composeapp.generated.resources.content_desc_open_date_range_picker
import plzstop.composeapp.generated.resources.export_date_picker_mode_range
import plzstop.composeapp.generated.resources.export_date_picker_mode_single
import plzstop.composeapp.generated.resources.export_date_range_label
import plzstop.composeapp.generated.resources.export_date_range_placeholder
import plzstop.composeapp.generated.resources.month_names_short

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangeField(
    startDateMillis: Long,
    endDateMillis: Long,
    onRangeChange: (start: Long, end: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val rangeText = formatDateRange(startDateMillis, endDateMillis)
    val openLabel = stringResource(Res.string.content_desc_open_date_range_picker)

    Column {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedInvertedTextField(
            value = rangeText,
            onValueChange = {},
            modifier = modifier
                .noRippleClickable(
                    onClick = { showPicker = true },
                    role = Role.Button,
                )
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
            ),
        )
    }

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

private enum class DateSelectionMode {
    SINGLE,
    RANGE,
}

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
        DateSelectionMode.SINGLE ->
            singleState.selectedDateMillis != null

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
            ) {
                Text(stringResource(Res.string.add_expense_confirm))
            }
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

private fun DateSelectionMode.labelRes() = when (this) {
    DateSelectionMode.SINGLE -> Res.string.export_date_picker_mode_single
    DateSelectionMode.RANGE -> Res.string.export_date_picker_mode_range
}
