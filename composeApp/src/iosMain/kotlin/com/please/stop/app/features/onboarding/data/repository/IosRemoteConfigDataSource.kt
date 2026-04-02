package com.please.stop.app.features.onboarding.data.repository

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import com.please.stop.app.core.logger.logDebugWithTag
import kotlinx.cinterop.ExperimentalForeignApi
import com.please.stop.app.core.logger.logErrorWithTag
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosRemoteConfigDataSource : RemoteConfigDataSource {

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
