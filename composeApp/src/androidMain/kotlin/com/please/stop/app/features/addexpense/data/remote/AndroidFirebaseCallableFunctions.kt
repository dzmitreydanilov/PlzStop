package com.please.stop.app.features.addexpense.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AndroidFirebaseCallableFunctions : FirebaseCallableFunctions {

    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west1")

    @Suppress("UNCHECKED_CAST")
    override suspend fun call(
        functionName: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> = runCatching {
        val result = functions
            .getHttpsCallable(functionName)
            .call(data)
            .await()
        result.data as Map<String, Any?>
    }
}
