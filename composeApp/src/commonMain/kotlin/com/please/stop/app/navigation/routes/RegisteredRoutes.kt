package com.please.stop.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

fun PolymorphicModuleBuilder<NavKey>.registerGlobalRotes() {
    registerBottomNavBarStartRoute()
    registerGlobalRoutes()
}

private fun PolymorphicModuleBuilder<NavKey>.registerGlobalRoutes() {
    subclass(AuthRoute::class)
    subclass(UserRoute::class)
    subclass(OnboardingRoute::class)
    subclass(CreateExpenseRoute::class)
    subclass(EditExpenseRoute::class)
    subclass(ReceiptItemsRoute::class)
    subclass(CategoriesRoute::class)
    subclass(ArchivedCategoriesRoute::class)
    subclass(ExportRoute::class)
    subclass(SubscriptionsListRoute::class)
}

fun PolymorphicModuleBuilder<NavKey>.registerBottomNavBarStartRoute() {
    subclass(MainBottomTabs.Home::class)
    subclass(MainBottomTabs.Operations::class)
    subclass(MainBottomTabs.Analytics::class)
    subclass(MainBottomTabs.Settings::class)
}
