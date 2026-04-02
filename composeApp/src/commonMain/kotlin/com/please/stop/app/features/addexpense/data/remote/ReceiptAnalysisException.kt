package com.please.stop.app.features.addexpense.data.remote

sealed class ReceiptAnalysisException(message: String) : Exception(message) {
    class Unreadable(message: String) : ReceiptAnalysisException(message)
    class ServiceUnavailable(message: String) : ReceiptAnalysisException(message)
}
