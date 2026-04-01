package com.please.stop.app.network

import kotlinx.serialization.json.Json

internal val JsonSerializer = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    prettyPrint = true
    explicitNulls = false
    useArrayPolymorphism = false
    ignoreUnknownKeys = true
}
