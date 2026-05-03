package com.please.stop.app.features.auth.data

import com.please.stop.app.core.INotificationPermission
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

internal class IosNotificationPermission : INotificationPermission {

    override suspend fun isGranted(): Boolean = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus == UNAuthorizationStatusAuthorized)
            }
    }

    override suspend fun request(): Boolean = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                continuation.resume(granted)
            }
    }
}
