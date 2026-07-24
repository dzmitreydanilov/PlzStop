package com.please.stop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.navigation.RootContent
import com.please.stop.app.navigation.deeplink.DeepLinkHandler
import com.please.stop.app.navigation.deeplink.DeepLinkResolver
import com.please.stop.app.presentation.RootState
import com.please.stop.app.presentation.RootStateHolder
import com.please.stop.app.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    deepLinkUri: String? = null,
    deepLinkHandler: DeepLinkHandler? = null,
) {
    AppTheme {
        val rootStateHolder = koinViewModel<RootStateHolder>()
        val state by rootStateHolder.state.collectAsStateWithLifecycle()
        val retainedDeepLinkHandler = retain<DeepLinkHandler> { DeepLinkHandler(DeepLinkResolver()) }
        val resolvedDeepLinkHandler = deepLinkHandler ?: retainedDeepLinkHandler

        when (val s = state) {
            is RootState.Loading -> RootLoadingSurface()
            is RootState.Ready -> RootContent(
                initialRoute = s.initialRoute,
                deepLinkUri = deepLinkUri,
                deepLinkHandler = resolvedDeepLinkHandler,
            )
        }
    }
}

@Composable
private fun RootLoadingSurface() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}
