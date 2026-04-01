package com.please.stop.app

import android.app.Application
import com.please.stop.app.core.logger.initLogger
import com.please.stop.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class PleaseStopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initLogger()
        initKoin(
            config = {
                androidContext(this@PleaseStopApplication)
            }
        )
    }
}
