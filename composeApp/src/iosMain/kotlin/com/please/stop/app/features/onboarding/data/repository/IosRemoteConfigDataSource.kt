package com.please.stop.app.features.onboarding.data.repository

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import com.please.stop.app.core.logger.logDebugWithTag
import com.please.stop.app.core.logger.logErrorWithTag
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosRemoteConfigDataSource : RemoteConfigDataSource {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchString(key: String): String? {
        return try {
            val remoteConfig = FIRRemoteConfig.remoteConfig()
            fetchAndActivate(remoteConfig)
            val value = remoteConfig.configValueForKey(key).stringValue
            value?.takeIf { it.isNotBlank() }.also {
                logDebugWithTag(tag = TAG, message = "Remote Config key=$key, hasValue=${it != null}")
            }
        } catch (e: Exception) {
            logErrorWithTag(tag = TAG, message = "Remote Config fetch failed for key=$key", throwable = e)
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchBoolean(key: String): Boolean {
        return try {
            val remoteConfig = FIRRemoteConfig.remoteConfig()
            fetchAndActivate(remoteConfig)
            remoteConfig.configValueForKey(key).boolValue
        } catch (e: Exception) {
            logErrorWithTag(tag = TAG, message = "Remote Config fetchBoolean failed for key=$key", throwable = e)
            false
        }
    }

    private suspend fun fetchAndActivate(remoteConfig: FIRRemoteConfig) {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchAndActivateWithCompletionHandler { _, error ->
                if (error != null) {
                    logErrorWithTag(tag = TAG, message = "fetchAndActivate error: ${error.localizedDescription}")
                }
                continuation.resume(Unit)
            }
        }
    }

    private companion object {
        const val TAG = "RemoteConfigDataSource"
    }
}
