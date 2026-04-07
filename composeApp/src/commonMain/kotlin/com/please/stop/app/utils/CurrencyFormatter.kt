package com.please.stop.app.utils

import kotlin.math.pow
import kotlin.math.roundToLong

const val DEFAULT_CURRENCY_DECIMAL_PLACES = 2

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

fun minorUnitsMultiplier(decimalPlaces: Int): Long = 10.0.pow(decimalPlaces).toLong()

fun minorUnitsToFloat(minorUnits: Long, decimalPlaces: Int): Float =
    (minorUnits.toDouble() / 10.0.pow(decimalPlaces)).toFloat()

fun Double.toMinorUnits(decimalPlaces: Int): Long = (this * 10.0.pow(decimalPlaces)).roundToLong()

private fun Double.toBigDecimalString(decimalPlaces: Int): String {
    val intPart = toLong()
    val fracPart = ((this - intPart) * 10.0.pow(decimalPlaces))
        .toLong()
        .toString()
        .padStart(decimalPlaces, '0')
    return "$intPart.$fracPart"
}
