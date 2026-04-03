package com.please.stop.app.utils

import kotlin.math.pow

fun formatCurrencyAmount(
    minorUnits: Long,
    currencySymbol: String,
    decimalPlaces: Int,
): String {
    if (decimalPlaces == 0) return "$currencySymbol$minorUnits"
    val divisor = 10.0.pow(decimalPlaces)
    val value = minorUnits / divisor
    val formatted = value.toBigDecimalString(decimalPlaces)
    return "$currencySymbol$formatted"
}

private fun Double.toBigDecimalString(decimalPlaces: Int): String {
    val intPart = toLong()
    val fracPart = ((this - intPart) * 10.0.pow(decimalPlaces))
        .toLong()
        .toString()
        .padStart(decimalPlaces, '0')
    return "$intPart.$fracPart"
}
