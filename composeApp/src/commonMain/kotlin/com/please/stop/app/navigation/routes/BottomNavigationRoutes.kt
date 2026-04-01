@file:Suppress("Filename", "MatchingDeclarationName")

package com.dog.care.navigation.routes

import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Stable
sealed interface MainBottomTabs : NavKey {

    @Serializable
    data object Home : MainBottomTabs

    @Serializable
    data object AiAssistant : MainBottomTabs

    @Serializable
    data object Calendar : MainBottomTabs

    @Serializable
    data object Articles : MainBottomTabs
}
