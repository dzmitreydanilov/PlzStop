package com.please.stop.app.kvs


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import kotlin.properties.ReadOnlyProperty

fun createDataStore(context: Context, prefFileName: String): DataStore<Preferences> {
    return createDataStore {
        context.filesDir.resolve(prefFileName).absolutePath
    }
}

fun <T> serializableDataStoreDelegate(
    prefFileName: String,
    serializer: Serializer<T>
): ReadOnlyProperty<Context, DataStore<T>> {
    return dataStore(
        fileName = prefFileName,
        serializer = serializer
    )
}
