package com.please.stop.app.core.models.presentation

import androidx.compose.ui.Alignment

sealed interface UiEffect {
    data class ShowMessage(
        val message: String,
        val alignment: Alignment = Alignment.BottomCenter,
    ) : UiEffect
}
