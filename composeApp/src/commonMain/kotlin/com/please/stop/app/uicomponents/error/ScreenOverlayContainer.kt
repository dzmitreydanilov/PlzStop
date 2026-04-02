package com.please.stop.app.uicomponents.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.uicomponents.NoTitleTopBar
import com.please.stop.app.uicomponents.buttons.ApplicationButton
import com.please.stop.app.uicomponents.buttons.SingleTopBarTextButton
import com.please.stop.app.uicomponents.snackbar.core.SnackbarBox
import com.please.stop.app.uicomponents.snackbar.core.SnackbarComponent
import com.please.stop.app.uicomponents.snackbar.core.SnackbarDuration
import com.please.stop.app.uicomponents.snackbar.core.SnackbarMessage
import com.please.stop.app.uicomponents.snackbar.ui.BottomBannerContent
import com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage
import com.please.stop.app.uicomponents.snackbar.ui.models.ErrorBannerMessage
import com.please.stop.app.uicomponents.snackbar.ui.models.InfoBannerMessage
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.close
import plzstop.composeapp.generated.resources.error_dialog_title
import plzstop.composeapp.generated.resources.error_occurred_internet
import plzstop.composeapp.generated.resources.error_occurred_we_are_fixing
import plzstop.composeapp.generated.resources.retry

@Composable
fun ScreenOverlayContainer(
    overlay: ScreenOverlay?,
    onDismiss: () -> Unit,
    onAutoDismiss: () -> Unit = {},
    onRetry: () -> Unit = {},
    successSnackbarDuration: SnackbarDuration = SnackbarDuration.Short,
    errorSnackbarDuration: SnackbarDuration = SnackbarDuration.Short,
    snackBarBottomPadding: Dp = 8.dp,
    snackbarComponent: SnackbarComponent<BannerMessage> = remember { SnackbarComponent() },
    content: @Composable () -> Unit,
) {
    val defaultErrorTitle = stringResource(Res.string.error_dialog_title)
    val defaultErrorSubTitle = stringResource(Res.string.error_occurred_we_are_fixing)

    LaunchedEffect(overlay) {
        when (overlay) {
            is ScreenOverlay.Error if overlay.type !is ErrorType.Network -> {
                snackbarComponent.show(
                    SnackbarMessage(
                        duration = errorSnackbarDuration,
                        content = ErrorBannerMessage(
                            title = overlay.title ?: defaultErrorTitle,
                            subtitle = overlay.subtitle ?: defaultErrorSubTitle,
                            onCloseClick = onDismiss,
                        ),
                        alignment = Alignment.BottomCenter,
                    )
                )
            }

            is ScreenOverlay.Message -> {
                snackbarComponent.show(
                    SnackbarMessage(
                        duration = successSnackbarDuration,
                        content = InfoBannerMessage(
                            title = overlay.title,
                            subtitle = overlay.subtitle.orEmpty(),
                        ),
                        alignment = when (overlay.position) {
                            SnackbarPosition.Top -> Alignment.TopCenter
                            SnackbarPosition.Bottom -> Alignment.BottomCenter
                        },
                    )
                )
            }

            else -> snackbarComponent.hide()
        }
    }

    SnackbarBox(
        modifier = Modifier.systemBarsPadding(),
        onDismiss = onAutoDismiss,
        bottomPadding = snackBarBottomPadding,
        component = snackbarComponent,
        snackbarContent = { message ->
            BottomBannerContent(
                message = message,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        content()
        when (overlay) {
            is ScreenOverlay.Error -> {
                if (overlay.type is ErrorType.Network) {
                    FullScreenDialog(
                        showDialog = true,
                        onDismissRequest = onDismiss,
                        topAppBarContent = {
                            NoTitleTopBar(
                                actions = {
                                    SingleTopBarTextButton(
                                        text = stringResource(Res.string.close),
                                        onClick = onDismiss,
                                    )
                                },
                            )
                        },
                        content = {
                            ErrorContent(
                                errorType = overlay.type,
                                onRetryClick = onRetry,
                            )
                        },
                    )
                }
            }

            else -> { /* No dialog needed */ }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit = {},
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = true),
    content: @Composable () -> Unit,
    topAppBarContent: @Composable () -> Unit = {},
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
        ) {
            val density = LocalDensity.current
            val statusBarHeight = WindowInsets.statusBars.getTop(density).dp
            val navigatorBarHeight = WindowInsets.navigationBars.getBottom(density).dp
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(
                        top = statusBarHeight,
                        bottom = navigatorBarHeight,
                    ),
                ) {
                    topAppBarContent()
                    content()
                }
            }
        }
    }
}

@Composable
fun ErrorContent(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.Unknown(stringResource(Res.string.error_dialog_title)),
    causeText: String = getCauseError(errorType),
    onRetryButtonVisible: Boolean = true,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = causeText, textAlign = TextAlign.Center)
        }

        if (onRetryButtonVisible) {
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                ApplicationButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRetryClick,
                ) {
                    Text(text = stringResource(Res.string.retry))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun getCauseError(errorType: ErrorType): String {
    val resId = when (errorType) {
        is ErrorType.Network -> Res.string.error_occurred_internet
        else -> Res.string.error_occurred_we_are_fixing
    }
    return stringResource(resId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun FullScreenPreview() {
    MaterialTheme {
        FullScreenDialog(
            showDialog = true,
            onDismissRequest = {},
            topAppBarContent = {
                TopAppBar(title = { Text("Preview Title") })
            },
            content = {
                ErrorContent(onRetryClick = {})
            },
        )
    }
}

@Preview
@Composable
private fun ErrorContentPreview() {
    MaterialTheme {
        Surface {
            ErrorContent(onRetryClick = {})
        }
    }
}

@Preview
@Composable
private fun ErrorContentNoNetworkPreview() {
    MaterialTheme {
        Surface {
            ErrorContent(
                onRetryClick = {},
                errorType = ErrorType.Network("Hello"),
            )
        }
    }
}
