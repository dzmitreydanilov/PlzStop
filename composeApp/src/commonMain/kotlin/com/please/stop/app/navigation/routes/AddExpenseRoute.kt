package com.please.stop.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AddExpenseRoute(
    val expenseId: Long? = null,
    val preselectedCategoryId: Long? = null,
) : NavKey
