package com.please.stop.app.features.addexpense.data.remote

interface FirebaseCallableFunctions {
    suspend fun call(functionName: String, data: Map<String, Any?>): Result<Map<String, Any?>>
}
