package com.please.stop.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.navigation.RootContent
import com.please.stop.app.presentation.RootState
import com.please.stop.app.presentation.RootStateHolder
import com.please.stop.app.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    AppTheme {
        val rootStateHolder = koinViewModel<RootStateHolder>()
        val state by rootStateHolder.state.collectAsStateWithLifecycle()

        when (val s = state) {
            is RootState.Loading -> SplashScreen()
            is RootState.Ready -> RootContent(initialRoute = s.initialRoute)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
