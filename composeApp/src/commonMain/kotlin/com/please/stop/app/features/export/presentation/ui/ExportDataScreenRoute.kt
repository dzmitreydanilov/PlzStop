package com.please.stop.app.features.export.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.export.domain.model.ExportDestination
import com.please.stop.app.features.export.domain.model.SpreadSheetFormat
import com.please.stop.app.features.export.presentation.ExportEvent
import com.please.stop.app.features.export.presentation.ExportState
import com.please.stop.app.features.export.presentation.ExportStateHolder
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.utils.date.nowMillis
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.close
import plzstop.composeapp.generated.resources.content_desc_navigate_back
import plzstop.composeapp.generated.resources.export_title
import plzstop.composeapp.generated.resources.ic_arrow_back

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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                            contentDescription = stringResource(Res.string.content_desc_navigate_back),
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(Res.string.export_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(Res.string.close))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                DocumentTitleInputField(
                    state = state,
                    onTitleChange = onEvent,
                )

                DateRangeField(
                    startDateMillis = state.currentStartDateMillis,
                    endDateMillis = state.currentEndDateMillis,
                    onRangeChange = { start, end ->
                        onEvent(ExportEvent.DateRangeSelected(start, end))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                DestinationSelector(
                    selected = state.currentDestination,
                    onSelect = { onEvent(ExportEvent.DestinationSelected(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                OrganizationMethod(
                    state = state,
                    onMethodSelect = onEvent,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExportActionContent(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                currentDestination = ExportDestination.GOOGLE_SHEETS,
                currentStartDateMillis = nowMillis(),
                currentEndDateMillis = nowMillis(),
                fileName = "My Expenses",
                hasExpensesToExport = true,
            ),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}
