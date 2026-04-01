package com.please.stop.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
actual fun getScreenSize(): ScreenSize {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val widthPx = windowInfo.containerSize.width
    val heightPx = windowInfo.containerSize.height

    return with(density) {
        ScreenSize(
            widthPx = widthPx,
            heightPx = heightPx,
            widthDp = widthPx.toDp(),
            heightDp = heightPx.toDp()
        )
    }
}
