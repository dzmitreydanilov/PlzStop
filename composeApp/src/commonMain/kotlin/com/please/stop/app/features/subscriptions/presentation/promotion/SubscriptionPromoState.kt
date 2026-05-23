package com.please.stop.app.features.subscriptions.presentation.promotion

sealed interface SubscriptionPromoState {
    data object Hidden : SubscriptionPromoState
    data object Visible : SubscriptionPromoState
}
