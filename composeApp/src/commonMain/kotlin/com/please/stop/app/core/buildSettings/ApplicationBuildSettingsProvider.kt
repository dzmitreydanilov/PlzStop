package com.please.stop.app.core.buildSettings

abstract class ApplicationBuildSettingsProvider : AppBuildSettingsProvider {

    override fun getPlatformName(): Platform {
        return platform
    }
}
