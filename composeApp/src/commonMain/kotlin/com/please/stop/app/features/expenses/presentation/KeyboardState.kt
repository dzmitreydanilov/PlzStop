package com.please.stop.app.features.expenses.presentation

data class KeyboardState(
    val displayExpression: String,
    val isInExpressionMode: Boolean,
    val currentValue: String,
)
