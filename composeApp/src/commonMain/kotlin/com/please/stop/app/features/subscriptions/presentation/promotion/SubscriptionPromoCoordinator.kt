package com.please.stop.app.features.subscriptions.presentation.promotion

import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.features.subscriptions.data.service.SubscriptionPromoConsumer
import com.please.stop.app.features.subscriptions.domain.usecase.DismissSubscriptionPromoUseCase
import com.please.stop.app.features.subscriptions.domain.usecase.ShouldShowSubscriptionPromoUseCase
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SubscriptionPromoCoordinator(
    private val promoConsumer: SubscriptionPromoConsumer,
    private val shouldShowPromo: ShouldShowSubscriptionPromoUseCase,
    private val dismissPromo: DismissSubscriptionPromoUseCase,
    private val scope: CoroutineScope,
) {

    val state: StateFlow<SubscriptionPromoState>
        field = MutableStateFlow<SubscriptionPromoState>(SubscriptionPromoState.Hidden)

    private val _navigation = Channel<Navigation>(Channel.BUFFERED)
    val navigation: Flow<Navigation> = _navigation.receiveAsFlow()

    private val isCollecting = atomic(false)

    fun start() {
        if (isCollecting.compareAndSet(expect = false, update = true)) {
            collectTriggers()
        }
    }

    fun stop() {
        isCollecting.value = false
        scope.coroutineContext.cancelChildren()
    }

    fun onCtaClicked() {
        state.value = SubscriptionPromoState.Hidden
        scope.launch {
            dismissPromo()
            _navigation.send(NavigateToSubscriptionsList)
        }
    }

    fun onDismissed() {
        state.value = SubscriptionPromoState.Hidden
        scope.launch { dismissPromo() }
    }

    private fun collectTriggers() {
        scope.launch {
            promoConsumer.triggers.collect {
                handleTrigger()
            }
        }
    }

    private suspend fun handleTrigger() {
        if (state.value is SubscriptionPromoState.Visible) return

        val shouldShow = shouldShowPromo().first()
        if (shouldShow) {
            state.value = SubscriptionPromoState.Visible
        }
    }
}

internal data object NavigateToSubscriptionsList : Navigation
