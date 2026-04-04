package com.please.stop.app.utils

/**
 * Returns the ISO 4217 currency code for the device's current locale,
 * or null if it cannot be determined.
 */
expect fun getDeviceCurrencyCode(): String?
