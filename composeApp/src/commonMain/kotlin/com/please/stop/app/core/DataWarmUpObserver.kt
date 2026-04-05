package com.please.stop.app.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.please.stop.app.core.coroutines.ICoroutineScopeProvider
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

class DataWarmUpObserver(
    private val currencyRepository: CurrencyRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        warmUp()
    }

    fun warmUp() {
        scopeProvider.getScope().launch(ioDispatcher) {
            currencyRepository.getAllCurrencies()
        }
    }
}
