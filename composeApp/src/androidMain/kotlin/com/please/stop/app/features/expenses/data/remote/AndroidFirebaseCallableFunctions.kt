package com.please.stop.app.features.expenses.data.remote

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AndroidFirebaseCallableFunctions : FirebaseCallableFunctions {

    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west1")

    @Suppress("UNCHECKED_CAST")
    override suspend fun call(
        functionName: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> = try {
        val result = functions.getHttpsCallable(functionName).call(data).await()
        Result.success(result.data as Map<String, Any?>)
    } catch (error: CancellationException) {
        throw error
    } catch (error: FirebaseFunctionsException) {
        val details = error.details as? Map<*, *>
        Result.failure(
            FirebaseCallableException(
                code = error.code.name,
                reason = (details?.get("reason") as? String)?.let(::FirebaseCallableErrorReason),
                message = error.message,
            )
        )
    } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
        Result.failure(error)
    }
}
