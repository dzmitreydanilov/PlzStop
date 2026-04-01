package com.please.stop.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun getScreenWidth(): Dp = UIScreen.mainScreen.bounds.useContents { size.width }.dp

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun getScreenHeight(): Dp = UIScreen.mainScreen.bounds.useContents { size.height }.dp
