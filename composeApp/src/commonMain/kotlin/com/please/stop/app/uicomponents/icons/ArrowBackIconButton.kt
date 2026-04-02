package com.please.stop.app.uicomponents.icons

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.content_desc_navigate_back
import plzstop.composeapp.generated.resources.ic_arrow_back

@Composable
fun ArrowBackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_arrow_back),
            tint = tint,
            contentDescription = stringResource(Res.string.content_desc_navigate_back)
        )
    }
}

@Preview
@Composable
private fun ArrowBackIconButtonPreview() {
    com.please.stop.app.uicomponents.icons.ArrowBackIconButton(onClick = {})
}
