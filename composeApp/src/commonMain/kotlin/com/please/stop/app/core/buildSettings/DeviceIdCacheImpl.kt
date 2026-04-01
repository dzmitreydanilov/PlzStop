package com.please.stop.app.core.buildSettings

import com.please.stop.app.core.DeviceDataStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class DeviceIdCacheImpl(
    private val storage: DeviceDataStorage
) : DeviceIdCache {

    private val mutex = Mutex()
    @Volatile
    private var cached: String? = null
    @Volatile
    private var initialized = false

    private suspend fun ensureLoaded() {
        if (initialized) return
        mutex.withLock {
            if (!initialized) {
                cached = storage.readDeviceUniqueId()
                initialized = true
            }
        }
    }

    override suspend fun get(): String? {
        ensureLoaded()
        return cached
    }

    override suspend fun set(id: String?) {
        mutex.withLock {
            if (id == null) storage.deleteDeviceUniqueId() else storage.writeDeviceUniqueId(id)
            cached = id
            initialized = true
        }
    }

    override suspend fun delete() {
        set(null)
    }

    override suspend fun invalidate() {
        mutex.withLock {
            cached = null
            initialized = false
        }
    }
}
