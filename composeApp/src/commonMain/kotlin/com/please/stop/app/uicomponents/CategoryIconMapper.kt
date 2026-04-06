@file:Suppress("MatchingDeclarationName")

package com.please.stop.app.uicomponents

data class CategoryIcon(val key: String, val emoji: String)

val allCategoryIcons: List<CategoryIcon> = listOf(
    CategoryIcon("ic_food", "\uD83C\uDF54"),
    CategoryIcon("ic_transport", "\uD83D\uDE8C"),
    CategoryIcon("ic_housing", "\uD83C\uDFE0"),
    CategoryIcon("ic_entertainment", "\uD83C\uDFAC"),
    CategoryIcon("ic_groceries", "\uD83D\uDED2"),
    CategoryIcon("ic_health", "\u2764\uFE0F"),
    CategoryIcon("ic_shopping", "\uD83D\uDECD\uFE0F"),
    CategoryIcon("ic_education", "\uD83D\uDCDA"),
    CategoryIcon("ic_subscriptions", "\uD83D\uDD14"),
    CategoryIcon("ic_coffee", "\u2615"),
    CategoryIcon("ic_gym", "\uD83C\uDFCB\uFE0F"),
    CategoryIcon("ic_pets", "\uD83D\uDC3E"),
    CategoryIcon("ic_gifts", "\uD83C\uDF81"),
    CategoryIcon("ic_travel", "\u2708\uFE0F"),
    CategoryIcon("ic_savings", "\uD83D\uDCB0"),
    CategoryIcon("ic_car", "\uD83D\uDE97"),
    CategoryIcon("ic_phone", "\uD83D\uDCF1"),
    CategoryIcon("ic_music", "\uD83C\uDFB5"),
    CategoryIcon("ic_games", "\uD83C\uDFAE"),
    CategoryIcon("ic_bills", "\uD83D\uDCB3"),
    CategoryIcon("ic_charity", "\uD83E\uDD1D"),
    CategoryIcon("ic_kids", "\uD83D\uDC76"),
    CategoryIcon("ic_tools", "\uD83D\uDD27"),
    CategoryIcon("ic_other", "\uD83D\uDCCC"),
)

private val emojiMap: Map<String, String> = allCategoryIcons.associate { it.key to it.emoji }

fun categoryEmojiForKey(iconKey: String): String = emojiMap[iconKey] ?: "\uD83D\uDCCC"
