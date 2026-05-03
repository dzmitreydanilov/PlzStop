package com.please.stop.app.features.auth.data

import kotlin.experimental.ExperimentalObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("IosFcmTokenBridge", exact = true)
interface IosFcmTokenBridge {
    fun getToken(onSuccess: (String) -> Unit, onError: (String) -> Unit)
}
