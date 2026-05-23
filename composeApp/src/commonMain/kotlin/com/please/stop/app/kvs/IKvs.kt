package com.please.stop.app.kvs

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface IKvs {
    suspend fun saveLong(key: Preferences.Key<Long>, value: Long)
    suspend fun saveString(key: Preferences.Key<String>, value: String)
    suspend fun saveInt(key: Preferences.Key<Int>, value: Int)
    suspend fun saveBoolean(key: Preferences.Key<Boolean>, value: Boolean)

    fun getBoolean(key: Preferences.Key<Boolean>): Flow<Boolean>
    fun getLong(key: Preferences.Key<Long>): Flow<Long?>
    fun getString(key: Preferences.Key<String>): Flow<String?>
    fun getInt(key: Preferences.Key<Int>): Flow<Int?>

    suspend fun remove(key: Preferences.Key<*>)
    suspend fun clear()
}
