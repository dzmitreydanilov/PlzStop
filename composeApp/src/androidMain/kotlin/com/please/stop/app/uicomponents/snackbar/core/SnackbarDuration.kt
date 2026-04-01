package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.runtime.Stable

@Stable
sealed interface SnackbarDuration {

    val timeInMillis: Long

    data object Short : SnackbarDuration {
        override val timeInMillis: Long = 3000
    }

    data object Permanent : SnackbarDuration {
        override val timeInMillis: Long = Long.MAX_VALUE
    }

    data class Custom(override val timeInMillis: Long) : SnackbarDuration
}
