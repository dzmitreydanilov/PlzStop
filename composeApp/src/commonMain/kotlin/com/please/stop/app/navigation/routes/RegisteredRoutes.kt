package com.please.stop.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass

fun PolymorphicModuleBuilder<NavKey>.registerGlobalRotes() {
    registerBottomNavBarStartRoute()
    registerGlobalRoutes()
}

private fun PolymorphicModuleBuilder<NavKey>.registerGlobalRoutes() {
    subclass(OnboardingRoute::class)
    subclass(CreateExpenseRoute::class)
    subclass(EditExpenseRoute::class)
    subclass(ReceiptItemsRoute::class)
    subclass(MonthlyExpensesRoute::class)
    subclass(CategoriesRoute::class)
    subclass(ArchivedCategoriesRoute::class)
}

fun PolymorphicModuleBuilder<NavKey>.registerBottomNavBarStartRoute() {
    subclass(MainBottomTabs.Home::class)
}
