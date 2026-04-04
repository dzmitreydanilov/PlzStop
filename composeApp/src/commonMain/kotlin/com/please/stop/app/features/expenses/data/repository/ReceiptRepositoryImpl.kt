package com.please.stop.app.features.expenses.data.repository

import co.touchlab.kermit.Logger
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import com.please.stop.app.features.expenses.data.remote.ReceiptAnalysisException
import com.please.stop.app.features.expenses.domain.model.ExpenseCategory
import com.please.stop.app.features.expenses.domain.model.ExpenseSubcategory
import com.please.stop.app.features.expenses.domain.model.ReceiptData
import com.please.stop.app.features.expenses.domain.repository.ReceiptRepository
import kotlinx.coroutines.withTimeout
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.pow
import kotlin.math.roundToLong

class ReceiptRepositoryImpl(
    private val callableFunctions: FirebaseCallableFunctions,
) : ReceiptRepository {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun analyzeReceipt(
        imageBytes: ByteArray,
        categories: List<ExpenseCategory>,
        decimalPlaces: Int,
        subcategories: List<ExpenseSubcategory>,
    ): Result<ReceiptData> = runCatching {
        val imageBase64 = Base64.encode(imageBytes)
        log.d { "Image size: ${imageBytes.size} bytes, base64 length: ${imageBase64.length}" }

        val categoriesData = categories.map { category ->
            mapOf("id" to category.id, "name" to category.name)
        }
        log.d { "Sending ${categoriesData.size} categories: $categoriesData" }

        val requestData = buildMap<String, Any> {
            put("imageBase64", imageBase64)
            put("categories", categoriesData)
            if (subcategories.isNotEmpty()) {
                val subcategoriesData = subcategories.map { sub ->
                    mapOf(
                        "id" to sub.id,
                        "parentCategoryId" to sub.parentCategoryId,
                        "name" to sub.name,
                    )
                }
                put("subcategories", subcategoriesData)
                log.d { "Sending ${subcategoriesData.size} subcategories" }
            }
        }

        log.d { "Calling analyzeReceipt..." }
        val callResult = withTimeout(TIMEOUT_MS) {
            callableFunctions.call("analyzeReceipt", requestData)
        }
        log.d { "Call result: isSuccess=${callResult.isSuccess}, isFailure=${callResult.isFailure}" }
        callResult.onFailure { log.e(it) { "Function call failed" } }
        val response = callResult.getOrThrow()
        log.d { "Response: $response" }

        val statusRaw = response["status"] as? String
        val data = response["data"] as? Map<*, *>
        val message = response["message"] as? String
        val status = ResponseStatus.fromValue(statusRaw)
        log.d { "Parsed: status=$status, data=$data, message=$message" }

        when (status) {
            ResponseStatus.NOT_RECEIPT -> throw ReceiptAnalysisException.NotReceipt(
                message ?: "Image is not a receipt."
            )
            ResponseStatus.UNREADABLE -> throw ReceiptAnalysisException.Unreadable(
                message ?: "Couldn't read this receipt."
            )
            ResponseStatus.ERROR, ResponseStatus.UNKNOWN -> throw ReceiptAnalysisException.ServiceUnavailable(
                message ?: "Service error processing receipt."
            )
            ResponseStatus.SUCCESS, ResponseStatus.PARTIAL -> {
                val totalAmount = (data?.get("totalAmount") as? Number)?.toDouble()
                val totalAmountMinorUnits = totalAmount?.let {
                    (it * 10.0.pow(decimalPlaces)).roundToLong()
                }

                ReceiptData(
                    merchantName = data?.get("merchantName") as? String,
                    totalAmountMinorUnits = totalAmountMinorUnits,
                    currency = data?.get("currency") as? String,
                    date = data?.get("date") as? String,
                    categoryId = (data?.get("categoryId") as? Number)?.toLong(),
                    subcategoryId = (data?.get("subcategoryId") as? Number)?.toLong(),
                    isPartial = status == ResponseStatus.PARTIAL,
                    message = message,
                )
            }
        }
    }

    private enum class ResponseStatus(val value: String) {
        SUCCESS("success"),
        PARTIAL("partial"),
        NOT_RECEIPT("not_receipt"),
        UNREADABLE("unreadable"),
        ERROR("error"),
        UNKNOWN("");

        companion object {
            fun fromValue(value: String?): ResponseStatus =
                entries.firstOrNull { it.value == value } ?: UNKNOWN
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
        val log = Logger.withTag("ReceiptRepository")
    }
}
