package com.please.stop.app.features.expenses.presentation

import com.please.stop.app.utils.minorUnitsMultiplier
import kotlin.math.abs
import kotlin.math.roundToLong

class KeyboardCalculator(
    private val decimalPlaces: Int,
    private val currencySymbol: String,
) {

    private val multiplier: Long = minorUnitsMultiplier(decimalPlaces)

    private var accumulator: Double = 0.0
    private var pendingOperator: KeyboardOperator? = null
    private var currentOperand: String = ""
    private var didJustEvaluate: Boolean = false
    private var isNegativePrefix: Boolean = false

    fun processKey(key: NumericKey): KeyboardState = when (key) {
        is NumericKey.Digit -> handleDigit(key.value)
        is NumericKey.Decimal -> handleDecimal()
        is NumericKey.Backspace -> handleBackspace()
        is NumericKey.Operator -> handleOperator(key.op)
        is NumericKey.Equals -> handleEquals()
        is NumericKey.Calendar,
        is NumericKey.Notes,
        is NumericKey.CurrencySymbol -> getState()
    }

    fun getState(): KeyboardState = KeyboardState(
        displayExpression = buildDisplayExpression(),
        isInExpressionMode = pendingOperator != null,
        currentValue = getAmountForSave(),
    )

    fun setFromAmount(amountInput: String) {
        resetAll()
        currentOperand = amountInput
    }

    fun getAmountForSave(): String {
        if (pendingOperator != null) {
            return formatDouble(accumulator)
        }
        if (isNegativePrefix && currentOperand.isNotEmpty()) {
            return "-$currentOperand"
        }
        return currentOperand
    }

    fun parseToMinorUnits(): Long {
        val input = getAmountForSave()
        if (input.isEmpty()) return 0L
        val value = input.toDoubleOrNull() ?: return 0L
        return (value * multiplier).roundToLong()
    }

    fun formatFromMinorUnits(minorUnits: Long): String {
        if (decimalPlaces == 0) return minorUnits.toString()
        val intPart = minorUnits / multiplier
        val fracPart = (minorUnits % multiplier)
            .toString()
            .padStart(decimalPlaces, '0')
        return "$intPart.$fracPart"
    }

    private fun handleDigit(digit: Int): KeyboardState {
        didJustEvaluate = false
        val candidate = currentOperand + digit
        if (isValidOperand(candidate)) {
            currentOperand = candidate
        }
        return getState()
    }

    private fun handleDecimal(): KeyboardState {
        if (decimalPlaces == 0) return getState()
        didJustEvaluate = false
        if (currentOperand.contains('.')) return getState()
        currentOperand = if (currentOperand.isEmpty()) "0." else "$currentOperand."
        return getState()
    }

    private fun handleBackspace(): KeyboardState {
        if (currentOperand.isNotEmpty()) {
            currentOperand = currentOperand.dropLast(1)
        } else if (isNegativePrefix && pendingOperator == null) {
            isNegativePrefix = false
        } else if (pendingOperator != null) {
            pendingOperator = null
            currentOperand = formatDouble(accumulator)
            accumulator = 0.0
        }
        return getState()
    }

    private fun handleOperator(op: KeyboardOperator): KeyboardState {
        didJustEvaluate = false

        val isExpressionEmpty = pendingOperator == null &&
            currentOperand.isEmpty() &&
            !isNegativePrefix

        if (op == KeyboardOperator.SUBTRACT && isExpressionEmpty) {
            isNegativePrefix = true
            return getState()
        }

        if (currentOperand.isEmpty()) {
            pendingOperator = op
            return getState()
        }

        val operandValue = parseCurrentOperand()
        val currentOp = pendingOperator
        accumulator = if (currentOp != null) {
            clampResult(evaluate(currentOp, operandValue), currentOp)
        } else {
            operandValue
        }

        pendingOperator = op
        currentOperand = ""
        isNegativePrefix = false
        return getState()
    }

    private fun handleEquals(): KeyboardState {
        val op = pendingOperator ?: return getState()

        val operandValue = if (currentOperand.isEmpty()) 0.0 else parseCurrentOperand()
        val result = evaluate(op, operandValue)
        val clampedResult = clampResult(result, op)
        val formatted = formatDouble(clampedResult)

        resetAll()
        currentOperand = formatted
        didJustEvaluate = true
        return getState()
    }

    private fun evaluate(op: KeyboardOperator, right: Double): Double = when (op) {
        KeyboardOperator.ADD -> accumulator + right
        KeyboardOperator.SUBTRACT -> accumulator - right
        KeyboardOperator.MULTIPLY -> accumulator * right
        KeyboardOperator.DIVIDE -> if (right == 0.0) 0.0 else accumulator / right
    }

    private fun clampResult(result: Double, op: KeyboardOperator): Double {
        if (op == KeyboardOperator.DIVIDE && result < 0.0) return 0.0
        return result.coerceIn(-MAX_AMOUNT_VALUE.toDouble(), MAX_AMOUNT_VALUE.toDouble())
    }

    private fun parseCurrentOperand(): Double {
        val value = currentOperand.toDoubleOrNull() ?: 0.0
        return if (isNegativePrefix) -value else value
    }

    private fun isValidOperand(input: String): Boolean {
        val dotIndex = input.indexOf('.')
        if (dotIndex >= 0) {
            val fracLength = input.length - dotIndex - 1
            if (fracLength > decimalPlaces) return false
        }
        val numericValue = input.toDoubleOrNull() ?: return false
        return numericValue <= MAX_AMOUNT_VALUE
    }

    private fun formatDouble(value: Double): String {
        if (decimalPlaces == 0) return value.toLong().toString()
        val absValue = abs(value)
        val intPart = absValue.toLong()
        val fracPart = ((absValue - intPart) * multiplier).roundToLong()
            .toString()
            .padStart(decimalPlaces, '0')
            .trimEnd('0')
        val prefix = if (value < 0) "-" else ""
        return if (fracPart.isEmpty()) "$prefix$intPart" else "$prefix$intPart.$fracPart"
    }

    private fun buildDisplayExpression(): String {
        if (isNegativePrefix && currentOperand.isEmpty() && pendingOperator == null) {
            return "- 0 $currencySymbol"
        }

        val sb = StringBuilder()

        if (pendingOperator != null) {
            sb.append("${formatDouble(accumulator)} $currencySymbol")
            sb.append(' ')
            sb.append(operatorSymbol(pendingOperator))
        }

        if (currentOperand.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(' ')
            if (isNegativePrefix && pendingOperator == null) sb.append("- ")
            sb.append("$currentOperand $currencySymbol")
        }

        return sb.toString()
    }

    private fun operatorSymbol(op: KeyboardOperator?): String = when (op) {
        KeyboardOperator.ADD -> "+"
        KeyboardOperator.SUBTRACT -> "-"
        KeyboardOperator.MULTIPLY -> "×"
        KeyboardOperator.DIVIDE -> "÷"
        null -> ""
    }

    private fun resetAll() {
        accumulator = 0.0
        pendingOperator = null
        currentOperand = ""
        didJustEvaluate = false
        isNegativePrefix = false
    }

    companion object {
        private const val MAX_AMOUNT_VALUE = 9_999_999
    }
}
