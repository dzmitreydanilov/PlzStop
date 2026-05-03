package com.please.stop.app

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.please.stop.app.core.logger.initLogger
import com.please.stop.app.di.initKoin
import com.please.stop.app.features.export.data.KoinWorkerFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

class PleaseStopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initLogger()

        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebug) {
            Firebase.appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        initKoin(
            config = {
                androidContext(this@PleaseStopApplication)
            }
        )

        WorkManager.initialize(
            this,
            Configuration.Builder()
                .setWorkerFactory(KoinWorkerFactory())
                .build(),
        )

        val lifecycleObservers: List<DefaultLifecycleObserver> = GlobalContext.get().getAll()
        lifecycleObservers.forEach {
            ProcessLifecycleOwner.get().lifecycle.addObserver(it)
        }
    }
}
