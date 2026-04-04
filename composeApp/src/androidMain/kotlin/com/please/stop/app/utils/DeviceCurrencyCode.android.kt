package com.please.stop.app.utils

import java.util.Currency
import java.util.Locale

actual fun getDeviceCurrencyCode(): String? {
    return try {
        Currency.getInstance(Locale.getDefault()).currencyCode
    } catch (_: IllegalArgumentException) {
        null
    }
}
