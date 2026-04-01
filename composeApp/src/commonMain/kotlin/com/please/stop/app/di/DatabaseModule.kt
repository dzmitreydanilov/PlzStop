package com.please.stop.app.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.core.db.AppDatabaseFactory
import org.koin.dsl.module

internal val databaseModule = module {
    single<AppDatabase> {
        get<AppDatabaseFactory>().create()
    }
}
