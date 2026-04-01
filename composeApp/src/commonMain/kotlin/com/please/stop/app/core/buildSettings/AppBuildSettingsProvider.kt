package com.please.stop.app.core.buildSettings

interface AppBuildSettingsProvider {

    fun isDebug(): Boolean

    fun getApplicationVersion(): String

    fun getLanguage(): String

    fun getApplicationName(): String

    fun getOSVersion(): String

    fun getPlatformName(): Platform

    fun getApplicationID(): String
}
