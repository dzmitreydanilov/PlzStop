@file:Suppress("NoUnusedImports")

package com.please.stop.app.uicomponents.snackbar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.please.stop.app.uicomponents.error.MessageType
import com.please.stop.app.uicomponents.icons.CloseIconButton
import com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage
import com.please.stop.app.uicomponents.snackbar.ui.models.ErrorBannerMessage
import com.please.stop.app.uicomponents.snackbar.ui.models.InfoBannerMessage
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_error
import plzstop.composeapp.generated.resources.ic_success_circle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomBannerContent(
    message: com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage,
    modifier: Modifier = Modifier
) {
    when (message) {
        is com.please.stop.app.uicomponents.snackbar.ui.models.ErrorBannerMessage ->
            com.please.stop.app.uicomponents.snackbar.ui.ErrorBannerMessageContent(
                modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
                title = message.title,
                subtitle = message.subtitle,
                onCloseClick = { message.onCloseClick() }
            )

        is com.please.stop.app.uicomponents.snackbar.ui.models.InfoBannerMessage ->
            com.please.stop.app.uicomponents.snackbar.ui.InfoBannerMessageContent(
                modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
                title = message.title,
                type = com.please.stop.app.uicomponents.error.MessageType.Success,
                subtitle = message.subtitle,
                onCloseClick = { message.onCloseClick() }
            )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InfoBannerMessageContent(
    title: String,
    type: com.please.stop.app.uicomponents.error.MessageType,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
            Row {
                Column(
                    Modifier
                        .padding(16.dp)
                        .weight(1f)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_success_circle),
                            contentDescription = null,
                        )
                        com.please.stop.app.uicomponents.snackbar.ui.AutoSizedTitle(
                            text = title
                        )
                    }

                    com.please.stop.app.uicomponents.snackbar.ui.AutoSizedSubtitle(
                        text = subtitle
                    )
                }
                if (type != com.please.stop.app.uicomponents.error.MessageType.Success) {
                    com.please.stop.app.uicomponents.icons.CloseIconButton(
                        onClick = onCloseClick
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                thickness = 4.dp
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ErrorBannerMessageContent(
    title: String,
    subtitle: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row {
                Column(
                    Modifier
                        .padding(16.dp)
                        .weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_error),
                            contentDescription = null,
                        )
                        com.please.stop.app.uicomponents.snackbar.ui.AutoSizedTitle(
                            text = title
                        )
                    }
                    com.please.stop.app.uicomponents.snackbar.ui.AutoSizedSubtitle(
                        text = subtitle
                    )
                }
                com.please.stop.app.uicomponents.icons.CloseIconButton(onClick = onCloseClick)
            }
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                thickness = 4.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AutoSizedSubtitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 4.dp, start = 24.dp),
        style = MaterialTheme.typography.bodyLargeEmphasized,
        autoSize = TextAutoSize.StepBased(maxFontSize = MaterialTheme.typography.bodyLargeEmphasized.fontSize)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AutoSizedTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLargeEmphasized.copy(fontWeight = FontWeight.SemiBold),
        autoSize = TextAutoSize.StepBased(maxFontSize = MaterialTheme.typography.bodyLargeEmphasized.fontSize)
    )
}


@Preview
@Composable
private fun InfoBottomBannerContentPreview() {
    com.please.stop.app.uicomponents.snackbar.ui.BottomBannerContent(
        message = com.please.stop.app.uicomponents.snackbar.ui.models.InfoBannerMessage(
            title = "Title",
            subtitle = "Subtitle"
        )
    )
}

@Preview
@Composable
private fun ErrorBottomBannerContentPreview() {
    com.please.stop.app.uicomponents.snackbar.ui.BottomBannerContent(
        message = com.please.stop.app.uicomponents.snackbar.ui.models.ErrorBannerMessage(
            title = "Title",
            subtitle = "Subtitle",
            onCloseClick = {}
        )
    )
}
