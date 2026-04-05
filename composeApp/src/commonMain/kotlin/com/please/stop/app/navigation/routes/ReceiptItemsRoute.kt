package com.please.stop.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptItemsRoute(
    val merchantName: String?,
    val currency: String?,
    val dateString: String?,
    val categoryId: Long?,
    val subcategoryId: Long?,
    val isManualEntry: Boolean = false,
) : NavKey
