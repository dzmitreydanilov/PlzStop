package com.dog.care.navigation.routes

import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.routes.AuthRoute
import com.please.stop.app.navigation.routes.LegalDocsRoute
import com.please.stop.app.navigation.routes.NoNetworkRoute
import com.please.stop.app.navigation.routes.OnboardingRoute
import com.please.stop.app.navigation.routes.PaywallRoute
import com.please.stop.app.navigation.routes.QuestionnaireRoute
import com.please.stop.app.navigation.routes.RemindersRoutes
import com.please.stop.app.navigation.routes.SymptomsRoute
import com.please.stop.app.navigation.routes.UserRoutes
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

fun PolymorphicModuleBuilder<NavKey>.registerGlobalRotes() {
    registerBottomNavBarStartRoute()
    registerAuthRoutes()
    registerTasksRoutes()
    registerUserRoutes()
    registerGlobalRoutes()
    subclass(NoNetworkRoute::class)
}

private fun PolymorphicModuleBuilder<NavKey>.registerUserRoutes() {
    subclass(UserRoutes.Profile::class)
    subclass(UserRoutes.DogInfo::class)
    subclass(UserRoutes.SubscriptionDetails::class)
    subclass(UserRoutes.Settings::class)
}

private fun PolymorphicModuleBuilder<NavKey>.registerGlobalRoutes() {
    subclass(PaywallRoute::class)
    subclass(LegalDocsRoute::class)
    subclass(QuestionnaireRoute::class)
    subclass(OnboardingRoute::class)
    subclass(SymptomsRoute::class)
}

private fun PolymorphicModuleBuilder<NavKey>.registerAuthRoutes() {
    subclass(AuthRoute.PreSignIn::class)
    subclass(AuthRoute.ProvideEmailCode::class)
    subclass(AuthRoute.ChooseSignInOption::class)
}

private fun PolymorphicModuleBuilder<NavKey>.registerTasksRoutes() {
    subclass(RemindersRoutes.SearchReminders::class)
    subclass(RemindersRoutes.CreateReminder::class)
    subclass(RemindersRoutes.EditReminder::class)
}

fun PolymorphicModuleBuilder<NavKey>.registerBottomNavBarStartRoute() {
    subclass(MainBottomTabs.Home::class)
}
