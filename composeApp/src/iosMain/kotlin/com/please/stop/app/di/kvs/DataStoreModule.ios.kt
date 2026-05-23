package com.please.stop.app.di.kvs

import com.please.stop.app.kvs.IKvs
import com.please.stop.app.kvs.Kvs
import com.please.stop.app.kvs.createDataStore
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val dataStoreModule: Module = module {
    single<IKvs>(named(KvsQualifiers.SubscriptionPromo.name)) {
        Kvs(dataStore = createDataStore(prefFileName = PROMO_DATASTORE_FILE))
    }
}
