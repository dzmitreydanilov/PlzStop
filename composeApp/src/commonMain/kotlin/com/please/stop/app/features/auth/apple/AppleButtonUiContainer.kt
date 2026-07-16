package com.please.stop.app.features.auth.apple

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.please.stop.app.features.auth.google.UiContainerScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppleButtonUiContainer(
    onAppleSignInResult: (AppleUser?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val appleAuthProvider = koinInject<AppleAuthProvider>()
    val coroutineScope = rememberCoroutineScope()
    val updatedOnResult by rememberUpdatedState(onAppleSignInResult)
    val uiContainerScope = remember(appleAuthProvider, coroutineScope) {
        object : UiContainerScope {
            override fun onClick() {
                coroutineScope.launch {
                    updatedOnResult(appleAuthProvider.signIn())
                }
            }
        }
    }

    Box(modifier = modifier) { uiContainerScope.content() }
}
