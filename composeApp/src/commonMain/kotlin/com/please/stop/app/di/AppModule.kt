package com.please.stop.app.di

import com.please.stop.app.di.dispatchers.dispatchersModule
import org.koin.dsl.module

internal val appModule = module {
    includes(
        platformModule,
        dispatchersModule,
        databaseModule,
    )
}
