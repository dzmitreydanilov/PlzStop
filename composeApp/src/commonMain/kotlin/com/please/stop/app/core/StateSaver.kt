package com.please.stop.app.core

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

interface StateSaver<S> {
    fun saveState(state: S, key: String)
    fun getState(defaultValue: S, key: String): S
}

class StateSaverImpl<S>(
    private val savedStateHandle: SavedStateHandle,
    private val serializer: KSerializer<S>
) : StateSaver<S> {

    private val json = Json { ignoreUnknownKeys = true }

    override fun saveState(state: S, key: String) {
        savedStateHandle[key] = json.encodeToString(serializer, state)
    }

    override fun getState(defaultValue: S, key: String): S {
        return savedStateHandle.get<String>(key)?.let {
            json.decodeFromString(serializer, it)
        } ?: defaultValue
    }
}
