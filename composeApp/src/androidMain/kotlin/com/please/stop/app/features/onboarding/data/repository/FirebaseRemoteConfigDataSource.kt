package com.please.stop.app.features.onboarding.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.please.stop.app.core.logger.logErrorWithTag

class FirebaseRemoteConfigDataSource(
    private val remoteConfig: FirebaseRemoteConfig,
) : RemoteConfigDataSource {

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

    private companion object {
        const val TAG = "RemoteConfigDataSource"
    }
}
