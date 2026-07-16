package com.please.stop.app.features.auth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Connects a user-triggered UI action to foreground Google Sheets authorization. */
@Composable
fun GoogleSheetsAuthorizationUiContainer(
    onAuthorizationResult: (GoogleSheetsAuthorizationCode?) -> Unit,
    modifier: Modifier = Modifier,
    forceConsent: Boolean = false,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val googleAuthProvider = koinInject<GoogleAuthProvider>()
    val googleAuthUiProvider = googleAuthProvider.getUiProvider()
    val coroutineScope = rememberCoroutineScope()
    val updatedOnResult by rememberUpdatedState(onAuthorizationResult)

    val uiContainerScope = remember(googleAuthUiProvider, forceConsent) {
        object : UiContainerScope {
            override fun onClick() {
                coroutineScope.launch {
                    updatedOnResult(googleAuthUiProvider.authorizeSheets(forceConsent = forceConsent))
                }
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}
