package com.please.stop.app.features.export.data

internal fun formatMinorUnits(minorUnits: Long, decimalPlaces: Int): String {
    require(decimalPlaces in 0..MAX_DECIMAL_PLACES) {
        "decimalPlaces must be between 0 and $MAX_DECIMAL_PLACES"
    }

    val rawValue = minorUnits.toString()
    if (decimalPlaces == 0) return rawValue

    val sign = if (minorUnits < 0) "-" else ""
    val digits = rawValue
        .removePrefix("-")
        .padStart(decimalPlaces + 1, '0')
    return "$sign${digits.dropLast(decimalPlaces)}.${digits.takeLast(decimalPlaces)}"
}

private const val MAX_DECIMAL_PLACES = 10
