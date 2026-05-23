package com.please.stop.app.features.export.presentation.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.auth.google.GoogleButtonUiContainer
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.presentation.ExportEvent
import com.please.stop.app.features.export.presentation.ExportState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.close
import plzstop.composeapp.generated.resources.export_button
import plzstop.composeapp.generated.resources.export_connect_google_body
import plzstop.composeapp.generated.resources.export_connect_google_button
import plzstop.composeapp.generated.resources.export_connect_google_title
import plzstop.composeapp.generated.resources.export_csv_share_launched
import plzstop.composeapp.generated.resources.export_enable_notifications_body
import plzstop.composeapp.generated.resources.export_enable_notifications_title
import plzstop.composeapp.generated.resources.export_enqueued_message
import plzstop.composeapp.generated.resources.export_failed_body
import plzstop.composeapp.generated.resources.export_failed_title
import plzstop.composeapp.generated.resources.export_no_expenses_body
import plzstop.composeapp.generated.resources.export_no_expenses_title
import plzstop.composeapp.generated.resources.export_title
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.retry

private const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

@Composable
internal fun ExportActionContent(
    state: ExportState,
    onEvent: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ExportState.Enqueued -> EnqueuedContent()
        is ExportState.CsvShareLaunched -> CsvShareLaunchedContent()
        is ExportState.NeedsGoogleAccount -> NeedsGoogleContent(onEvent = onEvent)
        is ExportState.Idle if !state.hasExpensesToExport -> NoExpensesContent(
            onDismiss = {
                onEvent(ExportEvent.Dismiss)
            },
        )

        is ExportState.Error -> ErrorContent(onEvent = onEvent)
        is ExportState.Confirm -> ConfirmContent(
            state = state,
            onEvent = onEvent,
            modifier = modifier,
        )

        is ExportState.Idle -> {
            Button(
                onClick = {
                    onEvent(
                        ExportEvent.StartExport(
                            startDateMillis = state.currentStartDateMillis,
                            endDateMillis = state.currentEndDateMillis,
                        ),
                    )
                },
                modifier = modifier,
            ) {
                Text(stringResource(Res.string.export_button))
            }
        }
    }
}

@Composable
private fun EnqueuedContent() {
    ExportSuccessContent(message = stringResource(Res.string.export_enqueued_message))
}

@Composable
private fun CsvShareLaunchedContent() {
    ExportSuccessContent(message = stringResource(Res.string.export_csv_share_launched))
}

@Composable
private fun ExportSuccessContent(message: String) {
    Icon(
        imageVector = vectorResource(Res.drawable.ic_check),
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = message,
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
        Button(
            onClick = ::onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.export_connect_google_button))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Composable
private fun NeedsNotificationContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.close))
    }
}

@Composable
private fun NoExpensesContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
    OutlinedButton(
        onClick = onDismiss,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.close))
    }
}

@Composable
private fun ErrorContent(
    onEvent: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.retry))
    }
}

@Suppress("UnusedPrivateMember")
@Composable
private fun ConfirmContent(
    state: ExportState,
    onEvent: (ExportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(Res.string.export_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(16.dp))
    if (state.currentDestination == ExportDestination.CSV) {
        Button(
            onClick = { onEvent(ExportEvent.ShareCsvOptionSelected) },
            modifier = modifier,
        ) {
            Text(stringResource(Res.string.export_button))
        }
        return
    }
    GoogleButtonUiContainer(
        modifier = modifier,
        filterByAuthorizedAccounts = false,
        scopes = listOf(SHEETS_SCOPE, DRIVE_FILE_SCOPE),
        onGoogleSignInResult = { user ->
            if (user?.accessToken != null) {
//                onEvent(ExportEvent.ConfirmExport(user.accessToken))
            }
        },
    ) {
        Button(
            onClick = ::onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.export_button))
        }
    }
}
