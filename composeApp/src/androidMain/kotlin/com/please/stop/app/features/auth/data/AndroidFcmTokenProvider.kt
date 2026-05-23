package com.please.stop.app.features.auth.data

import com.google.firebase.messaging.FirebaseMessaging
import com.please.stop.app.core.IFcmTokenProvider
import kotlinx.coroutines.tasks.await

internal class AndroidFcmTokenProvider : IFcmTokenProvider {

    override suspend fun getToken(): String? = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }.getOrNull()
}
