package com.please.stop.app.features.export.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.presentation.ExportEvent
import com.please.stop.app.theme.LocalAppDimens
import com.please.stop.app.uicomponents.previews.ApplicationPreviewThemeWrapper
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.export_drive_folder_name
import plzstop.composeapp.generated.resources.export_spread_sheet_title

@Composable
internal fun DocumentTitleInputField(
    fileName: String,
    folderName: String,
    destination: ExportDestination,
    onEvent: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    AnimatedVisibility(
        visible = destination == ExportDestination.GOOGLE_SHEETS,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            OutlinedTextField(
                value = fileName,
                onValueChange = { onEvent(ExportEvent.FileNameEntered(it.take(MAX_TITLE_LENGTH))) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.export_spread_sheet_title)) },
            )
            OutlinedTextField(
                value = folderName,
                onValueChange = { onEvent(ExportEvent.FolderNameEntered(it.take(MAX_FOLDER_NAME_LENGTH))) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.extraSmall),
                label = { Text(stringResource(Res.string.export_drive_folder_name)) },
                singleLine = true,
            )
        }
    }
}

@Composable
internal fun ColumnScope.OrganizationMethod(
    destination: ExportDestination,
    currentSpreadSheetFormat: SpreadSheetFormat,
    onMethodSelect: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = destination == ExportDestination.GOOGLE_SHEETS,
        modifier = modifier,
    ) {
        OrganizationMethodSelector(
            selected = currentSpreadSheetFormat,
            onSelect = { onMethodSelect(ExportEvent.TabLayoutSelected(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun DocumentTitleInputFieldPreview() {
    DocumentTitleInputField(
        fileName = "FileName",
        folderName = "PlzStop exports",
        destination = ExportDestination.GOOGLE_SHEETS,
        onEvent = {},
        modifier = Modifier.padding(8.dp)
    )
}

private const val MAX_TITLE_LENGTH = 200
private const val MAX_FOLDER_NAME_LENGTH = 100
