package com.please.stop.app.core.models.domain

interface Result

interface ErrorResult {
    val errorType: ErrorType
}
