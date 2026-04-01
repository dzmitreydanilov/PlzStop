package com.please.stop.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import com.dog.care.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.deeplink.DeepLinkable
import kotlinx.serialization.Serializable

sealed class AuthRoute : NavKey {
    @Serializable
    data object ChooseSignInOption : AuthRoute()

    @Serializable
    data object ProvideEmailCode : AuthRoute()

    @Serializable
    data object PreSignIn : AuthRoute()
}

sealed interface UserRoutes : NavKey {

    @Serializable
    data object Profile : NavKey, DeepLinkable {
        override val parentRoute: NavKey get() = MainBottomTabs.Home
    }

    @Serializable
    data object SubscriptionDetails : NavKey, DeepLinkable {
        override val parentRoute: NavKey get() = Profile
    }

    @Serializable
    data object Settings : NavKey

    @Serializable
    data object DogInfo : NavKey
}

sealed interface RemindersRoutes : NavKey, DeepLinkable {

    override val parentRoute: NavKey get() = MainBottomTabs.Home

    @Serializable
    data class CreateReminder(val date: String?, val fromHome: Boolean) : RemindersRoutes

    @Serializable
    data object SearchReminders : RemindersRoutes

    @Serializable
    data class EditReminder(val reminderId: String, val recurrenceId: String) : RemindersRoutes
}

sealed interface ChatRoutes : NavKey {

    @Serializable
    data object Chat : ChatRoutes
}


/**
 * Full Screen Routes
 */
@Serializable
data object QuestionnaireRoute : NavKey

@Serializable
data object OnboardingRoute : NavKey

@Serializable
data object LegalDocsRoute : NavKey

@Serializable
data object PaywallRoute : NavKey

@Serializable
data object SymptomsRoute : NavKey, DeepLinkable {
    override val parentRoute: NavKey get() = MainBottomTabs.Home
}

@Serializable
data class NoNetworkRoute(val onNetworkAppearRoute: NavKey) : NavKey
