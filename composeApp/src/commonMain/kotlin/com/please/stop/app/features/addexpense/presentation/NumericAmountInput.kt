package com.please.stop.app.features.addexpense.presentation

import kotlin.math.pow

class NumericAmountInput(private val decimalPlaces: Int) {

    fun applyKey(current: String, key: NumericKey): String = when (key) {
        is NumericKey.Backspace -> {
            if (current.isEmpty()) "" else current.dropLast(1)
        }
        is NumericKey.Decimal -> {
            if (decimalPlaces == 0) current
            else if (current.contains('.')) current
            else if (current.isEmpty()) "0."
            else "$current."
        }
        is NumericKey.Digit -> {
            val candidate = current + key.value
            if (!isValidAmount(candidate)) current
            else candidate
        }
    }

    fun parseToMinorUnits(input: String): Long {
        if (input.isEmpty()) return 0L
        val value = input.toDoubleOrNull() ?: return 0L
        return (value * 10.0.pow(decimalPlaces)).toLong()
    }

    fun formatFromMinorUnits(minorUnits: Long): String {
        if (decimalPlaces == 0) return minorUnits.toString()
        val divisor = 10.0.pow(decimalPlaces)
        val intPart = (minorUnits / divisor).toLong()
        val fracPart = (minorUnits % 10.0.pow(decimalPlaces).toLong())
            .toString()
            .padStart(decimalPlaces, '0')
        return "$intPart.$fracPart"
    }

    private fun isValidAmount(input: String): Boolean {
        val dotIndex = input.indexOf('.')
        if (dotIndex >= 0) {
            val fracLength = input.length - dotIndex - 1
            if (fracLength > decimalPlaces) return false
        }
        val numericValue = input.toDoubleOrNull() ?: return false
        return numericValue <= MAX_AMOUNT_VALUE
    }

    companion object {
        private const val MAX_AMOUNT_VALUE = 9_999_999
    }
}
