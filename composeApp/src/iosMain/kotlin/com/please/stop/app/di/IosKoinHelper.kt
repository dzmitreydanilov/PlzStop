package com.please.stop.app.di

import com.please.stop.app.features.addexpense.data.remote.FirebaseCallableFunctions
import com.please.stop.app.features.addexpense.data.remote.IosFirebaseCallableFunctions
import com.please.stop.app.features.addexpense.data.remote.IosFirebaseFunctionsCaller
import org.koin.core.module.Module
import org.koin.dsl.module

fun createIosPlatformOverrides(
    firebaseFunctionsCaller: IosFirebaseFunctionsCaller,
): Module = module {
    single<FirebaseCallableFunctions> {
        IosFirebaseCallableFunctions(caller = firebaseFunctionsCaller)
    }
}
