package com.please.stop.app.kvs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Kvs(private val dataStore: DataStore<Preferences>) : IKvs {

    override suspend fun saveLong(key: Preferences.Key<Long>, value: Long) {
        dataStore.updateData {
            it.toMutablePreferences().apply { this[key] = value }
        }
    }

    override suspend fun saveString(key: Preferences.Key<String>, value: String) {
        dataStore.updateData {
            it.toMutablePreferences().apply { this[key] = value }
        }
    }

    override suspend fun saveInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.updateData {
            it.toMutablePreferences().apply { this[key] = value }
        }
    }

    override suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.updateData {
            it.toMutablePreferences().apply { this[key] = value }
        }
    }

    override fun getBoolean(key: Preferences.Key<Boolean>): Flow<Boolean> {
        return dataStore.data.map { it[key] ?: false }
    }

    override fun getLong(key: Preferences.Key<Long>): Flow<Long?> {
        return dataStore.data.map { it[key] }
    }

    override fun getString(key: Preferences.Key<String>): Flow<String?> {
        return dataStore.data.map { it[key] }
    }

    override fun getInt(key: Preferences.Key<Int>): Flow<Int?> {
        return dataStore.data.map { it[key] }
    }

    override suspend fun remove(key: Preferences.Key<*>) {
        dataStore.edit { it.remove(key) }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
