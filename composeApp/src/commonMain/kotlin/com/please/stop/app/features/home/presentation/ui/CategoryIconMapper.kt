package com.please.stop.app.features.home.presentation.ui

internal fun categoryEmojiForKey(iconKey: String): String = when (iconKey) {
    "ic_food" -> "\uD83C\uDF54"          // 🍔
    "ic_transport" -> "\uD83D\uDE8C"      // 🚌
    "ic_housing" -> "\uD83C\uDFE0"        // 🏠
    "ic_entertainment" -> "\uD83C\uDFAC"  // 🎬
    "ic_groceries" -> "\uD83D\uDED2"      // 🛒
    "ic_health" -> "\u2764\uFE0F"         // ❤️
    "ic_shopping" -> "\uD83D\uDECD\uFE0F" // 🛍️
    "ic_education" -> "\uD83D\uDCDA"      // 📚
    "ic_subscriptions" -> "\uD83D\uDD14"  // 🔔
    "ic_other" -> "\uD83D\uDCCC"          // 📌
    else -> "\uD83D\uDCCC"                // 📌
}
