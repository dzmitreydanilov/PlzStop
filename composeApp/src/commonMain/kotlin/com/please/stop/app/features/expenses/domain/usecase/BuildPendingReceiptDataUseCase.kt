package com.please.stop.app.features.expenses.domain.usecase

import com.please.stop.app.features.expenses.domain.model.PendingReceiptData
import com.please.stop.app.features.expenses.domain.model.ReceiptData
import com.please.stop.app.features.expenses.domain.model.ReceiptExpenseItem

object BuildPendingReceiptDataUseCase {

    sealed interface Input {
        data class FromReceipt(val receiptData: ReceiptData) : Input
        data class Manual(
            val categoryId: Long?,
            val subcategoryId: Long?,
        ) : Input
    }

    operator fun invoke(input: Input): PendingReceiptData = when (input) {
        is Input.FromReceipt -> fromReceipt(input.receiptData)
        is Input.Manual -> manual(input.categoryId, input.subcategoryId)
    }

    private fun fromReceipt(data: ReceiptData): PendingReceiptData = PendingReceiptData(
        items = data.items.map { item ->
            ReceiptExpenseItem(
                id = item.name + "_" + item.amountMinorUnits,
                name = item.name,
                amountMinorUnits = item.amountMinorUnits,
                categoryId = item.categoryId ?: data.categoryId,
                subcategoryId = item.subcategoryId ?: data.subcategoryId,
            )
        },
        merchantName = data.merchantName,
        currency = data.currency,
        dateString = data.date,
        categoryId = data.categoryId,
        subcategoryId = data.subcategoryId,
        isManualEntry = false,
    )

    private fun manual(categoryId: Long?, subcategoryId: Long?): PendingReceiptData =
        PendingReceiptData(
            items = listOf(
                ReceiptExpenseItem(
                    id = "manual_0",
                    name = "",
                    amountMinorUnits = 0L,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                )
            ),
            merchantName = null,
            currency = null,
            dateString = null,
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            isManualEntry = true,
        )
}
