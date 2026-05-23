package com.please.stop.app.features.subscriptions.domain.usecase

import com.please.stop.app.features.subscriptions.data.storage.ISubscriptionPromoStorage
import com.please.stop.app.utils.date.nowMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class DismissSubscriptionPromoUseCase(
    private val promoStorage: ISubscriptionPromoStorage,
    private val dispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke() {
        withContext(dispatcher) {
            promoStorage.recordDismissal(nowMillis())
        }
    }
}
