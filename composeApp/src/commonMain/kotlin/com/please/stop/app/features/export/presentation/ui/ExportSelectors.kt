package com.please.stop.app.features.export.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.theme.LocalAppDimens
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.export_destination_csv
import plzstop.composeapp.generated.resources.export_destination_google_sheets
import plzstop.composeapp.generated.resources.export_destination_label
import plzstop.composeapp.generated.resources.export_organization_method_label
import plzstop.composeapp.generated.resources.export_organization_same_tab
import plzstop.composeapp.generated.resources.export_organization_separate_tabs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DestinationSelector(
    selected: ExportDestination,
    onSelect: (ExportDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.export_destination_label).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimens.extraSmall))
        val options = ExportDestination.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, destination ->
                SegmentedButton(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(stringResource(destination.labelRes())) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizationMethodSelector(
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

private fun SpreadSheetFormat.labelRes() = when (this) {
    SpreadSheetFormat.SINGLE_TAB -> Res.string.export_organization_same_tab
    SpreadSheetFormat.SEPARATE_TABS -> Res.string.export_organization_separate_tabs
}

private fun ExportDestination.labelRes() = when (this) {
    ExportDestination.GOOGLE_SHEETS -> Res.string.export_destination_google_sheets
    ExportDestination.CSV -> Res.string.export_destination_csv
}
