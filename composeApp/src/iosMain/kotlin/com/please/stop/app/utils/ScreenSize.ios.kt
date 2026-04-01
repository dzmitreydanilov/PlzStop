package com.please.stop.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun getScreenSize(): ScreenSize {
    val screen = UIScreen.mainScreen
    val scale = screen.scale

    val widthPoints = CGRectGetWidth(screen.bounds)
    val heightPoints = CGRectGetHeight(screen.bounds)

    return ScreenSize(
        widthPx = (widthPoints * scale).toInt(),
        heightPx = (heightPoints * scale).toInt(),
        widthDp = widthPoints.dp,
        heightDp = heightPoints.dp
    )
}
