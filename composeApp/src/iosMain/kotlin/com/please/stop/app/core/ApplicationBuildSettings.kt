package com.please.stop.app.core

import com.please.stop.app.core.buildSettings.ApplicationBuildSettingsProvider
import com.please.stop.app.core.logger.logError
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.kCFBundleVersionKey
import platform.Foundation.NSBundle
import platform.Foundation.NSDictionary
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dictionaryWithContentsOfFile
import platform.Foundation.languageCode
import platform.UIKit.UIDevice
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
class ApplicationBuildSettings : ApplicationBuildSettingsProvider() {

    companion object {
        private const val BUILD_SETTINGS_PATH = "NOT SET"
        private const val PLIST = "plist"
        private const val CDBundleDisplayNameKey = "CFBundleDisplayName"
        private const val CFBundleShortVersionsStringKey = "CFBundleShortVersionString"
    }

    override fun getApplicationName(): String {
        val mainBundle =
            NSBundle.mainBundle.pathForResource(BUILD_SETTINGS_PATH, PLIST).orEmpty()

        val plist = NSDictionary.dictionaryWithContentsOfFile(mainBundle)
        return (plist?.get(CDBundleDisplayNameKey) as? String).orEmpty()
    }

    override fun getApplicationVersion(): String {
        val nsDictionary = NSBundle.mainBundle.infoDictionary
        val appVersion = nsDictionary?.get(CFBundleShortVersionsStringKey) as? String
            ?: nsDictionary?.get(kCFBundleVersionKey) as? String

        if (appVersion == null) {
            logError(message = "app version not found")
        }

        return appVersion.orEmpty()
    }

    override fun getLanguage(): String {
        return NSLocale.currentLocale.languageCode
    }

    @OptIn(ExperimentalNativeApi::class)
    override fun isDebug(): Boolean {
        return Platform.isDebugBinary
    }

    override fun getOSVersion(): String {
        return UIDevice.currentDevice.systemVersion
    }

    override fun getApplicationID(): String {
        return NSBundle.mainBundle.bundleIdentifier.orEmpty()
    }
}
