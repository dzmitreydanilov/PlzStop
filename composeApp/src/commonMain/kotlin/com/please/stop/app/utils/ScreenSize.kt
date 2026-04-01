package com.please.stop.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

data class ScreenSize(
    val widthPx: Int,
    val heightPx: Int,
    val widthDp: Dp,
    val heightDp: Dp
)

@Composable
expect fun getScreenSize(): ScreenSize


val ScreenSizeValue: ScreenSize
    @Composable
    get() = getScreenSize()
