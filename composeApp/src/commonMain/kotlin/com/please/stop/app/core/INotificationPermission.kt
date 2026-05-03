package com.please.stop.app.core

interface INotificationPermission {
    suspend fun isGranted(): Boolean
    suspend fun request(): Boolean
}
