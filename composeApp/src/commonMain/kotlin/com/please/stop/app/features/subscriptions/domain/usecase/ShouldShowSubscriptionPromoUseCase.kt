package com.please.stop.app.features.subscriptions.domain.usecase

import com.please.stop.app.features.subscriptions.data.storage.ISubscriptionPromoStorage
import com.please.stop.app.utils.date.nowMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class ShouldShowSubscriptionPromoUseCase(
    private val promoStorage: ISubscriptionPromoStorage,
    private val dispatcher: CoroutineDispatcher,
) {

    operator fun invoke(): Flow<Boolean> {
        return combine(
            promoStorage.getDismissedTimestamp(),
            promoStorage.getShownCount(),
        ) { dismissedTimestamp, shownCount ->
            if (shownCount >= MAX_SHOW_COUNT) return@combine false

            if (dismissedTimestamp != null) {
                val elapsed = nowMillis() - dismissedTimestamp
                if (elapsed < COOLDOWN_MILLIS) return@combine false
            }

            true
        }.flowOn(dispatcher)
    }

    private companion object {
        const val MAX_SHOW_COUNT = 3
        const val COOLDOWN_DAYS = 7
        const val COOLDOWN_MILLIS = COOLDOWN_DAYS * 24 * 60 * 60 * 1000L
    }
}
