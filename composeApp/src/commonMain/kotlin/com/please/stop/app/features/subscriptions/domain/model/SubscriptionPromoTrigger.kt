package com.please.stop.app.features.subscriptions.domain.model

sealed interface SubscriptionPromoTrigger {
    data object ExpenseCreatedInSubscriptionCategory : SubscriptionPromoTrigger
    data object AppLaunch : SubscriptionPromoTrigger
}
