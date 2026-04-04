package com.please.stop.app.utils

import platform.Foundation.NSLocale
import platform.Foundation.currencyCode
import platform.Foundation.currentLocale

actual fun getDeviceCurrencyCode(): String? {
    return NSLocale.currentLocale.currencyCode
}
