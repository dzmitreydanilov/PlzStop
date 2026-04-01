package com.please.stop.app.navigation.nav3

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun platformOnBack(): () -> Unit {
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    return remember(backPressedDispatcher) {
        {
            backPressedDispatcher?.onBackPressed()
        }
    }
}
