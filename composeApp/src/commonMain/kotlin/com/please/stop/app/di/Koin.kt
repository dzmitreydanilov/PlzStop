package com.please.stop.app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    config: KoinAppDeclaration? = null,
    platformOverrides: Module? = null,
): KoinApplication {
    return startKoin {
        config?.invoke(this)
        val modules = mutableListOf(appModule)
        platformOverrides?.let { modules.add(it) }
        modules(modules)
    }
}
