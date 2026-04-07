@file:Suppress("Filename", "MatchingDeclarationName")

package com.please.stop.app.navigation.routes

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Stable
sealed interface MainBottomTabs : NavKey {

    @Serializable
    data object Home : MainBottomTabs

    @Serializable
    data object Operations : MainBottomTabs

    @Serializable
    data object Analytics : MainBottomTabs

    @Serializable
    data object Settings : MainBottomTabs
}
