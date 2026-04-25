package com.please.stop.app.features.subscriptions.di

import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.subscriptions.data.service.SubscriptionPromoConsumer
import com.please.stop.app.features.subscriptions.data.service.SubscriptionPromoEmitter
import com.please.stop.app.features.subscriptions.data.service.SubscriptionPromoService
import com.please.stop.app.features.subscriptions.data.storage.ISubscriptionPromoStorage
import com.please.stop.app.features.subscriptions.data.storage.SubscriptionPromoStorage
import com.please.stop.app.features.subscriptions.domain.usecase.DismissSubscriptionPromoUseCase
import com.please.stop.app.features.subscriptions.domain.usecase.ShouldShowSubscriptionPromoUseCase
import com.please.stop.app.features.subscriptions.presentation.promotion.SubscriptionPromoCoordinator
import com.please.stop.app.kvs.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

private const val PROMO_DATASTORE_FILE = "subscription_promo.preferences_pb"

val subscriptionPromotionModule = module {

    single<ISubscriptionPromoStorage> {
        SubscriptionPromoStorage(
            dataStore = createDataStore { PROMO_DATASTORE_FILE },
        )
    }

    single {
        SubscriptionPromoService()
    } binds arrayOf(SubscriptionPromoEmitter::class, SubscriptionPromoConsumer::class)

    factory {
        ShouldShowSubscriptionPromoUseCase(
            promoStorage = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        DismissSubscriptionPromoUseCase(
            promoStorage = get(),
            dispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    single {
        SubscriptionPromoCoordinator(
            promoConsumer = get(),
            shouldShowPromo = get(),
            dismissPromo = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }
}
