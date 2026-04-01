package com.please.stop.app.di.dispatchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val dispatchersModule = module {
    single(named(DispatchersQualifiers.IO.name)) { Dispatchers.IO }
    single(named(DispatchersQualifiers.Main.name)) { Dispatchers.Main }
    single(named(DispatchersQualifiers.Default.name)) { Dispatchers.Default }
}

enum class DispatchersQualifiers {
    IO, Main, Default
}