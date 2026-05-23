package com.please.stop.app.features.export.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportExpenseRow(
    val date: String,
    val title: String,
    val category: String,
    val subcategory: String,
    val amount: String,
    val notes: String,
)
