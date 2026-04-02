package com.please.stop.app.navigation.nav3

import androidx.compose.runtime.Composable

@Composable
actual fun BackInterceptionProvider(
    interceptionEnabled: Boolean,
    content: @Composable (() -> Unit)
) {
    content()
}
