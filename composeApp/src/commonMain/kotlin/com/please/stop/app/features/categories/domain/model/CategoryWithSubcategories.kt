package com.please.stop.app.features.categories.domain.model

import com.please.stop.app.features.onboarding.domain.model.Category
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import kotlinx.collections.immutable.ImmutableList

/**
 * @param subcategories `null` when the subcategories feature is disabled,
 *   empty list when enabled but no subcategories exist yet.
 */
data class CategoryWithSubcategories(
    val category: Category,
    val subcategories: ImmutableList<Subcategory>?,
)
