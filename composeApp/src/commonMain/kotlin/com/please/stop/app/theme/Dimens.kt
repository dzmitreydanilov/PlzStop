package com.please.stop.app.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimens(
    val extraSmall: Dp = 0.dp,
    val small1: Dp = 0.dp,
    val small2: Dp = 0.dp,
    val small3: Dp = 0.dp,
    val medium1: Dp = 0.dp,
    val medium2: Dp = 0.dp,
    val medium3: Dp = 0.dp,
    val large: Dp = 0.dp,
    val extraLarge: Dp = 0.dp
)

val CompactDimens = Dimens(
    extraSmall = 8.dp,
    small1 = 12.dp,
    small2 = 16.dp,
    small3 = 20.dp,
    medium1 = 28.dp,
    medium2 = 36.dp,
    medium3 = 44.dp,
    large = 54.dp,
    extraLarge = 64.dp
)

val MediumDimens = Dimens(
    extraSmall = 10.dp,
    small1 = 14.dp,
    small2 = 18.dp,
    small3 = 22.dp,
    medium1 = 30.dp,
    medium2 = 38.dp,
    medium3 = 46.dp,
    large = 56.dp,
)

val ExpandedDimens = Dimens(
    extraSmall = 12.dp,
    small1 = 18.dp,
    small2 = 24.dp,
    small3 = 30.dp,
    medium1 = 40.dp,
    medium2 = 48.dp,
    medium3 = 56.dp,
    large = 66.dp,
)
