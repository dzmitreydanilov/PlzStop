package com.please.stop.app.core.buildSettings

interface DeviceIdCache : DeviceIdReader, DeviceIdMutator

interface DeviceIdReader {
    suspend fun get(): String?
}

interface DeviceIdMutator {
    suspend fun set(id: String?)
    suspend fun delete()
    suspend fun invalidate()
}
