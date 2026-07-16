package com.please.stop.app.uicomponents.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import com.please.stop.app.theme.AppTheme

class ApplicationPreviewThemeWrapper : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        AppTheme {
            content()
        }
    }
}
