package com.please.stop.app.features.expenses.data.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("IosFirebaseFunctionsCaller", exact = true)
interface IosFirebaseFunctionsCaller {
    fun callFunction(
        name: String,
        data: Map<String, Any>,
        onSuccess: (Map<String, Any>) -> Unit,
        onError: (String) -> Unit,
    )
}

class IosFirebaseCallableFunctions(
    private val caller: IosFirebaseFunctionsCaller,
) : FirebaseCallableFunctions {

    @Suppress("UNCHECKED_CAST")
    override suspend fun call(
        functionName: String,
        data: Map<String, Any?>,
    ): Result<Map<String, Any?>> = runCatching {
        suspendCancellableCoroutine { continuation ->
            val nonNullData = data.filterValues { it != null } as Map<String, Any>
            caller.callFunction(
                name = functionName,
                data = nonNullData,
                onSuccess = { result ->
                    continuation.resume(result)
                },
                onError = { error ->
                    continuation.resumeWithException(Exception(error))
                },
            )
        }
    }
}
