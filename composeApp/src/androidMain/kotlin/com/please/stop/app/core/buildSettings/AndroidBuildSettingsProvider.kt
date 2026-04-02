package com.please.stop.app.core.buildSettings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

class AndroidBuildSettingsProvider(
    private val context: Context,
    private val debug: Boolean,
) : ApplicationBuildSettingsProvider() {

    override fun isDebug(): Boolean = debug

    override fun getApplicationVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }

    override fun getLanguage(): String =
        context.resources.configuration.locales[0].language

    override fun getApplicationName(): String =
        context.applicationInfo.loadLabel(context.packageManager).toString()

    override fun getOSVersion(): String = Build.VERSION.RELEASE

    override fun getApplicationID(): String = context.packageName
}
