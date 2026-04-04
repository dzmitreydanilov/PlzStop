package com.please.stop.app.uicomponents.snackbar.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration

internal class DefaultSnackbarComponent<T>(
    override val animationDuration: Duration
) : com.please.stop.app.uicomponents.snackbar.core.SnackbarComponent<T> {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var showingJob: Job? = null
    private var dismissingJob: Job? = null

    final override val state: StateFlow<com.please.stop.app.uicomponents.snackbar.core.SnackbarState<T>>
        field = MutableStateFlow<com.please.stop.app.uicomponents.snackbar.core.SnackbarState<T>>(
            com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden(previousMessageContent = null)
        )

    override fun show(message: com.please.stop.app.uicomponents.snackbar.core.SnackbarContent<T>) {
        when (val currentState = state.value) {
            is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden<T> -> {
                showMessage(message)
            }

            is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Shown<T> -> {
                dismissMessageAndShow(
                    previousMessageContent = currentState.messageContent,
                    message = message
                )
            }
        }
    }

    override fun hide() {
        when (val currentState = state.value) {
            is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden<T> -> Unit
            is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Shown<T> -> {
                state.value = com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden(
                    previousMessageContent = currentState.messageContent
                )
            }
        }
    }

    override fun onDestroy() {
        coroutineScope.cancel()
    }

    private fun dismissMessageAndShow(
        previousMessageContent: com.please.stop.app.uicomponents.snackbar.core.SnackbarContent<T>?,
        message: com.please.stop.app.uicomponents.snackbar.core.SnackbarContent<T>
    ) {
        if (dismissingJob?.isActive == true) return

        dismissingJob = coroutineScope.launch {
            state.value = com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden(
                previousMessageContent = previousMessageContent
            )
            delay(animationDuration)
            showMessage(message = message)
        }
    }

    private fun showMessage(message: com.please.stop.app.uicomponents.snackbar.core.SnackbarContent<T>) {
        dismissingJob?.cancel()
        showingJob?.cancel()
        showingJob = coroutineScope.launch {
            val appliedState = com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Shown(message)
            state.value = appliedState
            delay(message.duration.timeInMillis)
            state.value = com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden(
                previousMessageContent = appliedState.messageContent
            )
        }
    }
}
