package com.please.stop.app.navigation.routes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class CreateExpenseRoute(val categoryId: Long? = null) : NavKey

@Serializable
data class EditExpenseRoute(val expenseId: Long) : NavKey
