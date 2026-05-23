package com.please.stop.app

import androidx.compose.ui.window.ComposeUIViewController
import com.please.stop.app.navigation.deeplink.DeepLinkHandler

@Suppress("FunctionNaming")
fun MainViewController(
    deepLinkUri: String? = null,
    deepLinkHandler: DeepLinkHandler? = null,
) = ComposeUIViewController {
    App(
        deepLinkUri = deepLinkUri,
        deepLinkHandler = deepLinkHandler,
    )
}
