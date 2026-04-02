package com.please.stop.app.uicomponents.icons

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.content_desc_close
import plzstop.composeapp.generated.resources.ic_close

@Composable
fun CloseIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            vectorResource(Res.drawable.ic_close),
            contentDescription = stringResource(Res.string.content_desc_close)
        )
    }
}

@Preview
@Composable
private fun CloseIconButtonPreview() {
    com.please.stop.app.uicomponents.icons.CloseIconButton({})
}
