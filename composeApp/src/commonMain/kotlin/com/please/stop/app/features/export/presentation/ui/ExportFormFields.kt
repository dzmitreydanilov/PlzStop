package com.please.stop.app.features.export.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.presentation.ExportEvent
import com.please.stop.app.features.export.presentation.ExportState
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.export_spread_sheet_title

@Composable
internal fun DocumentTitleInputField(
    state: ExportState,
    onTitleChange: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.currentDestination == ExportDestination.GOOGLE_SHEETS,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        OutlinedTextField(
            value = state.fileName.orEmpty(),
            onValueChange = { onTitleChange(ExportEvent.FileNameEntered(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.export_spread_sheet_title)) },
        )
    }
}

@Composable
internal fun ColumnScope.OrganizationMethod(
    state: ExportState,
    onMethodSelect: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.currentDestination == ExportDestination.GOOGLE_SHEETS,
        modifier = modifier,
    ) {
        OrganizationMethodSelector(
            selected = state.currentSpreadSheetFormat,
            onSelect = { onMethodSelect(ExportEvent.TabLayoutSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
