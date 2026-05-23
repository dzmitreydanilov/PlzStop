package com.please.stop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.please.stop.app.navigation.deeplink.DeepLinkHandler
import com.please.stop.app.navigation.deeplink.DeepLinkResolver
import com.please.stop.app.presentation.RootState
import com.please.stop.app.presentation.RootStateHolder
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val rootStateHolder: RootStateHolder by viewModel()
    private var coldStartDeepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        coldStartDeepLinkUri = intent?.dataString

        splashScreen.setKeepOnScreenCondition {
            rootStateHolder.state.value is RootState.Loading
        }

        setContent {
            App(deepLinkUri = coldStartDeepLinkUri)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
