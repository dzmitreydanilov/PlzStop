package com.please.stop.app.features.expenses.domain.usecase

object ResolveExpenseSaveAmountsUseCase {

    data class Input(
        val enteredAmountMinorUnits: Long,
        val convertedAmountMinorUnits: Long?,
        val currencyCode: String,
        val defaultCurrencyCode: String,
        val conversionRate: Double?,
        val saveInOriginalCurrency: Boolean,
    )

    data class Output(
        val amountToSave: Long,
        val originalAmountMinorUnits: Long?,
        val originalCurrencyCode: String?,
        val conversionRate: Double?,
    )

    operator fun invoke(input: Input): Output {
        val isForeignCurrency = input.currencyCode != input.defaultCurrencyCode &&
            input.conversionRate != null

        if (!isForeignCurrency) {
            return Output(
                amountToSave = input.enteredAmountMinorUnits,
                originalAmountMinorUnits = null,
                originalCurrencyCode = null,
                conversionRate = null,
            )
        }

        return if (input.saveInOriginalCurrency) {
            Output(
                amountToSave = input.enteredAmountMinorUnits,
                originalAmountMinorUnits = input.convertedAmountMinorUnits,
                originalCurrencyCode = input.defaultCurrencyCode,
                conversionRate = input.conversionRate,
            )
        } else {
            Output(
                amountToSave = input.convertedAmountMinorUnits ?: input.enteredAmountMinorUnits,
                originalAmountMinorUnits = input.enteredAmountMinorUnits,
                originalCurrencyCode = input.currencyCode,
                conversionRate = input.conversionRate,
            )
        }
    }
}
