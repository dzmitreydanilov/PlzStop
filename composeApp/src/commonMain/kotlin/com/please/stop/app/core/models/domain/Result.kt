package com.please.stop.app.core.models.domain

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("DomainResult", exact = true)
interface Result

interface ErrorResult {
    val errorType: ErrorType
}
