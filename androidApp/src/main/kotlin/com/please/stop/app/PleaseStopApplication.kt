package com.please.stop.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.please.stop.app.core.logger.initLogger
import com.please.stop.app.di.initKoin
import org.koin.android.ext.koin.androidContext

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
    }
}
