package com.please.stop.app.features.subscriptions.data.service

import com.please.stop.app.features.subscriptions.domain.model.SubscriptionPromoTrigger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface SubscriptionPromoEmitter {
    fun emit(trigger: SubscriptionPromoTrigger)
}

interface SubscriptionPromoConsumer {
    val triggers: SharedFlow<SubscriptionPromoTrigger>
}

class SubscriptionPromoService : SubscriptionPromoEmitter, SubscriptionPromoConsumer {

    private val _triggers = MutableSharedFlow<SubscriptionPromoTrigger>(
        extraBufferCapacity = BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val triggers: SharedFlow<SubscriptionPromoTrigger> = _triggers.asSharedFlow()

    override fun emit(trigger: SubscriptionPromoTrigger) {
        _triggers.tryEmit(trigger)
    }

    private companion object {
        const val BUFFER_CAPACITY = 16
    }
}
