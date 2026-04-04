package com.please.stop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.please.stop.app.presentation.RootState
import com.please.stop.app.presentation.RootStateHolder
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val rootStateHolder: RootStateHolder by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            rootStateHolder.state.value is RootState.Loading
        }

        setContent {
            App()
        }
    }
}
