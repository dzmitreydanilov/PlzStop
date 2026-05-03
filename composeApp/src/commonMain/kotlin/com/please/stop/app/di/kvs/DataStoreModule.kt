package com.please.stop.app.di.kvs

import org.koin.core.module.Module

internal const val PROMO_DATASTORE_FILE = "subscription_promo.preferences_pb"

expect val dataStoreModule: Module
