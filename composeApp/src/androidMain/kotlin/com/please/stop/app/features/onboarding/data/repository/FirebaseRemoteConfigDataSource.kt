package com.please.stop.app.features.onboarding.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.please.stop.app.core.logger.logErrorWithTag

class FirebaseRemoteConfigDataSource(
    private val remoteConfig: FirebaseRemoteConfig,
) : RemoteConfigDataSource {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchString(key: String): String? {
        return try {
            remoteConfig.fetchAndActivate()
            val value = remoteConfig.getValue(key).asString()
            value.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logErrorWithTag(
                tag = TAG,
                message = "Remote Config fetch failed for key=$key",
                throwable = e,
            )
            null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchBoolean(key: String): Boolean {
        return try {
            remoteConfig.fetchAndActivate()
            remoteConfig.getValue(key).asBoolean()
        } catch (e: Exception) {
            logErrorWithTag(
                tag = TAG,
                message = "Remote Config fetchBoolean failed for key=$key",
                throwable = e,
            )
            false
        }
    }

    private companion object {
        const val TAG = "RemoteConfigDataSource"
    }
}
