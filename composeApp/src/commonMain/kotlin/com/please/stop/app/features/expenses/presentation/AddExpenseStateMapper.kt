package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.features.expenses.domain.model.AddExpenseFormData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

object AddExpenseStateMapper {

    fun toCurrencyConfig(data: AddExpenseFormData): CurrencyConfig = CurrencyConfig(
        code = data.currencyCode,
        symbol = data.currencySymbol,
        decimalPlaces = data.decimalPlaces,
    )

    fun toCategoryUiModels(data: AddExpenseFormData): ImmutableList<CategoryUiModel> =
        data.categories.map { category ->
            CategoryUiModel(
                id = category.id,
                name = category.name,
                iconKey = category.iconKey,
            )
        }.toImmutableList()

    fun toSubcategoryUiModels(data: AddExpenseFormData): ImmutableList<SubcategoryUiModel> =
        data.subcategories.map { sub ->
            SubcategoryUiModel(
                id = sub.id,
                parentCategoryId = sub.parentCategoryId,
                name = sub.name,
                iconKey = sub.iconKey,
            )
        }.toImmutableList()
}
