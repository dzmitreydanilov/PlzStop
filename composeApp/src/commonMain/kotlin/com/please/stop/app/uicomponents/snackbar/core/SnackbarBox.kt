package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composable function to display a messenger box with customizable content and animations.
 * This function uses a [com.please.stop.app.uicomponents.snackbar.core.SnackbarComponent]
 * to manage the state and display of messages.
 *
 * @param component the [com.please.stop.app.uicomponents.snackbar.core.SnackbarComponent]
 *   responsible for managing and displaying messages.
 * @param snackbarContent the composable content for display message.
 * @param modifier the modifier to apply to the messenger box.
 * @param defaultAlignment fallback alignment when no message has been shown yet.
 *   Each message carries its own alignment via
 *   [com.please.stop.app.uicomponents.snackbar.core.SnackbarContent.alignment].
 * @param windowInsets the window insets to apply to the padding.
 * @param bottomPadding additional bottom padding between the snackbar and bottom edge (e.g., bottom bar).
 * @param onDismiss callback invoked when the snackbar disappears.
 * @param content the composable content to be displayed within the messenger box.
 */
@Composable
fun <T> SnackbarBox(
    component: com.please.stop.app.uicomponents.snackbar.core.SnackbarComponent<T>,
    modifier: Modifier = Modifier,
    defaultAlignment: Alignment = Alignment.BottomCenter,
    windowInsets: WindowInsets? = null,
    bottomPadding: Dp = 0.dp,
    onDismiss: (() -> Unit)? = null,
    snackbarContent: @Composable (content: T) -> Unit,
    content: @Composable () -> Unit,
) {
    val componentState by component.state.collectAsState()
    val bottomMessengerOffset = remember { mutableStateMapOf<String, Int>() }
    val topMessengerOffset = remember { mutableStateMapOf<String, Int>() }
    val layoutDirection = LocalLayoutDirection.current

    val messageAlignment = when (val state = componentState) {
        is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden ->
            state.previousMessageContent?.alignment ?: defaultAlignment
        is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Shown -> state.messageContent.alignment
    }

    var previousVisibility by remember { mutableStateOf(componentState.isVisible) }

    LaunchedEffect(componentState.isVisible) {
        if (previousVisibility && !componentState.isVisible) {
            onDismiss?.invoke()
        }
        previousVisibility = componentState.isVisible
    }

    CompositionLocalProvider(
        com.please.stop.app.uicomponents.snackbar.core.LocalBottomMessengerOffset provides bottomMessengerOffset,
        com.please.stop.app.uicomponents.snackbar.core.LocalTopMessengerOffset provides topMessengerOffset,
    ) {
        Box(modifier = modifier) {
            content()
            AnimatedVisibility(
                modifier = Modifier
                    .align(messageAlignment)
                    .fillMaxWidth(),
                visible = componentState.isVisible,
                enter = SnackbarBoxDefaults.enterTransition(
                    alignment = messageAlignment,
                    animationDuration = component.animationDuration
                ),
                exit = SnackbarBoxDefaults.exitTransition(
                    alignment = messageAlignment,
                    animationDuration = component.animationDuration
                ),
            ) {
                val message = when (val state = componentState) {
                    is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Hidden ->
                        state.previousMessageContent
                    is com.please.stop.app.uicomponents.snackbar.core.SnackbarState.Shown -> state.messageContent
                }
                val messageContent = message?.content
                if (messageContent != null) {
                    val additionalPadding = SnackbarBoxDefaults.animateAdditionalPadding(
                        alignment = messageAlignment,
                        windowInsets = windowInsets
                    )
                    Box(
                        Modifier.padding(
                            top = additionalPadding.calculateTopPadding(),
                            bottom = additionalPadding.calculateBottomPadding() + bottomPadding,
                            start = additionalPadding.calculateStartPadding(layoutDirection),
                            end = additionalPadding.calculateEndPadding(layoutDirection)
                        )
                    ) {
                        snackbarContent(messageContent)
                    }
                }
            }
        }
    }
}
