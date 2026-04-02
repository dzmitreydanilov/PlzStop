package com.please.stop.app.uicomponents.snackbar.ui.models

import androidx.compose.runtime.Stable

@Stable
interface BannerMessage {
    val title: String
    val subtitle: String
    val onCloseClick: () -> Unit
}

data class ErrorBannerMessage(
    override val title: String,
    override val subtitle: String,
    override val onCloseClick: () -> Unit
) : com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage

data class InfoBannerMessage(
    override val title: String,
    override val subtitle: String,
) : com.please.stop.app.uicomponents.snackbar.ui.models.BannerMessage {
    override val onCloseClick: () -> Unit = {}
}
