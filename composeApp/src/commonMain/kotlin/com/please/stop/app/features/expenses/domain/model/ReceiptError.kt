package com.please.stop.app.features.expenses.domain.model

import com.please.stop.app.core.models.domain.ErrorType

enum class ReceiptError {
    NOT_RECEIPT,
    UNREADABLE,
    NO_NETWORK,
    SERVICE_UNAVAILABLE,
}

fun ReceiptError.toErrorType(): ErrorType = when (this) {
    ReceiptError.NOT_RECEIPT -> ErrorType.Request(message = null)
    ReceiptError.UNREADABLE -> ErrorType.Request(message = null)
    ReceiptError.NO_NETWORK -> ErrorType.Network(message = null)
    ReceiptError.SERVICE_UNAVAILABLE -> ErrorType.Server(message = null)
}
