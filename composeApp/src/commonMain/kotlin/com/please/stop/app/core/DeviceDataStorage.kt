package com.please.stop.app.core

interface DeviceDataStorage {
    suspend fun writeFcmToken(token: String)
    suspend fun readFcmToken(): String?
    suspend fun deleteFcmToken()

    suspend fun writeDeviceUniqueId(id: String)
    suspend fun readDeviceUniqueId(): String?
    suspend fun deleteDeviceUniqueId()

    suspend fun deleteUserDeviceData()
}
