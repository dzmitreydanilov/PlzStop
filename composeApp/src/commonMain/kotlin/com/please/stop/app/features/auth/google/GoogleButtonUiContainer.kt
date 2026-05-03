package com.please.stop.app.features.auth.google

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.please.stop.app.features.auth.google.GoogleAuthUiProvider.Companion.BASIC_AUTH_SCOPE
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun GoogleButtonUiContainer(
    modifier: Modifier = Modifier,
    filterByAuthorizedAccounts: Boolean = true,
    isAutoSelectEnabled: Boolean = true,
    scopes: List<String> = BASIC_AUTH_SCOPE,
    onGoogleSignInResult: (GoogleUser?) -> Unit,
    content: @Composable UiContainerScope.() -> Unit,
) {
    val googleAuthProvider = koinInject<GoogleAuthProvider>()
    val googleAuthUiProvider = googleAuthProvider.getUiProvider()
    val coroutineScope = rememberCoroutineScope()
    val updatedOnResultFunc by rememberUpdatedState(onGoogleSignInResult)

    val uiContainerScope = remember {
        object : UiContainerScope {
            override fun onClick() {
                coroutineScope.launch {
                    val googleUser = googleAuthUiProvider.signIn(
                        filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                        isAutoSelectEnabled = isAutoSelectEnabled,
                        scopes = scopes
                    )
                    updatedOnResultFunc(googleUser)
                }
            }
        }
    }
    Box(modifier = modifier) { uiContainerScope.content() }
}
