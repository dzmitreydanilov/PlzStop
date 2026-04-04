package com.please.stop.app.features.expenses.data.remote

sealed class ReceiptAnalysisException(message: String) : Exception(message) {
    class NotReceipt(message: String) : ReceiptAnalysisException(message)
    class Unreadable(message: String) : ReceiptAnalysisException(message)
    class ServiceUnavailable(message: String) : ReceiptAnalysisException(message)
}
