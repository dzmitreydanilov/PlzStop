package com.please.stop.app.features.expenses.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyboardCalculatorTest {

    private fun calculator(decimalPlaces: Int = 2, symbol: String = "$") =
        KeyboardCalculator(decimalPlaces = decimalPlaces, currencySymbol = symbol)

    private fun KeyboardCalculator.pressDigits(vararg digits: Int): KeyboardState {
        var state = getState()
        digits.forEach { state = processKey(NumericKey.Digit(it)) }
        return state
    }

    private fun KeyboardCalculator.pressOperator(op: KeyboardOperator) =
        processKey(NumericKey.Operator(op))

    private fun KeyboardCalculator.pressEquals() = processKey(NumericKey.Equals)
    private fun KeyboardCalculator.pressBackspace() = processKey(NumericKey.Backspace)
    private fun KeyboardCalculator.pressDecimal() = processKey(NumericKey.Decimal)

    // region Digit input

    @Test
    fun singleDigit() {
        val calc = calculator()
        val state = calc.pressDigits(5)
        assertEquals("5 $", state.displayExpression)
        assertEquals("5", state.currentValue)
        assertFalse(state.isInExpressionMode)
    }

    @Test
    fun multipleDigits() {
        val calc = calculator()
        val state = calc.pressDigits(1, 2, 3)
        assertEquals("123 $", state.displayExpression)
        assertEquals("123", state.currentValue)
    }

    @Test
    fun maxValueRejectsExtraDigit() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(9, 9, 9, 9, 9, 9, 9)
        val state = calc.processKey(NumericKey.Digit(1))
        assertEquals("9999999", state.currentValue)
    }

    @Test
    fun digitAfterEqualsAppendsToResult() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(3)
        calc.pressEquals()
        val state = calc.pressDigits(7)
        assertEquals("87 $", state.displayExpression)
        assertEquals("87", state.currentValue)
        assertFalse(state.isInExpressionMode)
    }

    // endregion

    // region Decimal input

    @Test
    fun decimalAddsPoint() {
        val calc = calculator()
        calc.pressDigits(1)
        val state = calc.pressDecimal()
        assertEquals("1. $", state.displayExpression)
        assertEquals("1.", state.currentValue)
    }

    @Test
    fun decimalOnEmptyOperandPrefixesZero() {
        val calc = calculator()
        val state = calc.pressDecimal()
        assertEquals("0. $", state.displayExpression)
        assertEquals("0.", state.currentValue)
    }

    @Test
    fun secondDecimalIgnored() {
        val calc = calculator()
        calc.pressDigits(1)
        calc.pressDecimal()
        calc.pressDigits(5)
        val state = calc.pressDecimal()
        assertEquals("1.5", state.currentValue)
    }

    @Test
    fun decimalIgnoredWhenZeroDecimalPlaces() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(5)
        val state = calc.pressDecimal()
        assertEquals("5", state.currentValue)
    }

    @Test
    fun respectsDecimalPlacesLimit() {
        val calc = calculator(decimalPlaces = 2)
        calc.pressDigits(1)
        calc.pressDecimal()
        calc.pressDigits(2, 3)
        val state = calc.pressDigits(4)
        assertEquals("1.23", state.currentValue)
    }

    // endregion

    // region Operators

    @Test
    fun digitThenOperatorEntersExpressionMode() {
        val calc = calculator()
        calc.pressDigits(2, 5)
        val state = calc.pressOperator(KeyboardOperator.ADD)
        assertTrue(state.isInExpressionMode)
        assertTrue(state.displayExpression.contains("+"))
    }

    @Test
    fun chainedOperatorsEvaluateIntermediate() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(3)
        val state = calc.pressOperator(KeyboardOperator.ADD)
        assertTrue(state.displayExpression.contains("8"))
        assertTrue(state.isInExpressionMode)
    }

    @Test
    fun operatorSwitching() {
        val calc = calculator()
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        val state = calc.pressOperator(KeyboardOperator.MULTIPLY)
        assertTrue(state.displayExpression.contains("×"))
        assertFalse(state.displayExpression.contains("+"))
    }

    @Test
    fun subtractAsFirstKeyIsNegativePrefix() {
        val calc = calculator()
        val state = calc.pressOperator(KeyboardOperator.SUBTRACT)
        assertFalse(state.isInExpressionMode)
        assertEquals("- 0 $", state.displayExpression)
    }

    @Test
    fun backspaceClearsNegativePrefix() {
        val calc = calculator()
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        val state = calc.pressBackspace()
        assertEquals("", state.displayExpression)
        assertFalse(state.isInExpressionMode)
    }

    @Test
    fun negativePrefixThenDigits() {
        val calc = calculator()
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        val state = calc.pressDigits(2, 5)
        assertEquals("-25", state.currentValue)
        assertFalse(state.isInExpressionMode)
    }

    @Test
    fun subtractMidExpressionIsNormalOperator() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(1, 0)
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        assertTrue(calc.getState().isInExpressionMode)
    }

    // endregion

    // region Equals

    @Test
    fun basicAddition() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(3)
        val state = calc.pressEquals()
        assertEquals("8", state.currentValue)
        assertFalse(state.isInExpressionMode)
    }

    @Test
    fun basicSubtraction() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(1, 0)
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        calc.pressDigits(3)
        val state = calc.pressEquals()
        assertEquals("7", state.currentValue)
    }

    @Test
    fun basicMultiplication() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(4)
        calc.pressOperator(KeyboardOperator.MULTIPLY)
        calc.pressDigits(3)
        val state = calc.pressEquals()
        assertEquals("12", state.currentValue)
    }

    @Test
    fun basicDivision() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(1, 0)
        calc.pressOperator(KeyboardOperator.DIVIDE)
        calc.pressDigits(2)
        val state = calc.pressEquals()
        assertEquals("5", state.currentValue)
    }

    @Test
    fun divisionByZeroReturnsZero() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(1, 0)
        calc.pressOperator(KeyboardOperator.DIVIDE)
        calc.pressDigits(0)
        val state = calc.pressEquals()
        assertEquals("0", state.currentValue)
    }

    @Test
    fun divisionResultNegativeResetsToZero() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        calc.pressDigits(1, 0)
        calc.pressOperator(KeyboardOperator.DIVIDE)
        calc.pressDigits(2)
        val state = calc.pressEquals()
        assertEquals("0", state.currentValue)
    }

    @Test
    fun equalsWithNoPendingOperatorIsNoOp() {
        val calc = calculator()
        calc.pressDigits(5)
        val before = calc.getState()
        val after = calc.pressEquals()
        assertEquals(before.currentValue, after.currentValue)
        assertEquals(before.isInExpressionMode, after.isInExpressionMode)
    }

    @Test
    fun equalsWithNoSecondOperandUsesZero() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        val state = calc.pressEquals()
        assertEquals("5", state.currentValue)
    }

    @Test
    fun resultExceedingMaxIsClamped() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(9, 9, 9, 9, 9, 9, 9)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(9, 9, 9, 9, 9, 9, 9)
        val state = calc.pressEquals()
        assertEquals("9999999", state.currentValue)
    }

    @Test
    fun multipleEqualsPressesAreIdempotent() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(3)
        val first = calc.pressEquals()
        val second = calc.pressEquals()
        assertEquals(first.currentValue, second.currentValue)
    }

    // endregion

    // region Backspace

    @Test
    fun backspaceRemovesLastDigit() {
        val calc = calculator()
        calc.pressDigits(1, 2)
        val state = calc.pressBackspace()
        assertEquals("1", state.currentValue)
    }

    @Test
    fun backspaceCancelsOperatorAndRestoresAccumulator() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(2, 5)
        calc.pressOperator(KeyboardOperator.ADD)
        val state = calc.pressBackspace()
        assertFalse(state.isInExpressionMode)
        assertEquals("25", state.currentValue)
    }

    @Test
    fun backspaceOnEmptyIsNoOp() {
        val calc = calculator()
        val state = calc.pressBackspace()
        assertEquals("", state.currentValue)
        assertEquals("", state.displayExpression)
    }

    @Test
    fun backspaceRemovesDecimalPoint() {
        val calc = calculator()
        calc.pressDigits(5)
        calc.pressDecimal()
        val state = calc.pressBackspace()
        assertEquals("5", state.currentValue)
    }

    // endregion

    // region Negative values

    @Test
    fun negativeSubtractionIntermediateAllowed() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(3)
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        calc.pressDigits(5)
        val state = calc.pressEquals()
        assertEquals("-2", state.currentValue)
    }

    @Test
    fun negativeDivisionResetsToZero() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        calc.pressDigits(6)
        calc.pressOperator(KeyboardOperator.DIVIDE)
        calc.pressDigits(3)
        val state = calc.pressEquals()
        assertEquals("0", state.currentValue)
    }

    // endregion

    // region State restore

    @Test
    fun setFromAmountRestoresState() {
        val calc = calculator()
        calc.setFromAmount("25.50")
        val state = calc.getState()
        assertEquals("25.50 $", state.displayExpression)
        assertEquals("25.50", state.currentValue)
        assertFalse(state.isInExpressionMode)
    }

    @Test
    fun parseToMinorUnitsAfterInput() {
        val calc = calculator(decimalPlaces = 2)
        calc.pressDigits(1, 2)
        calc.pressDecimal()
        calc.pressDigits(5, 0)
        assertEquals(1250L, calc.parseToMinorUnits())
    }

    @Test
    fun parseToMinorUnitsAfterEquals() {
        val calc = calculator(decimalPlaces = 2)
        calc.pressDigits(1, 0)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(2)
        calc.pressDecimal()
        calc.pressDigits(5, 0)
        calc.pressEquals()
        assertEquals(1250L, calc.parseToMinorUnits())
    }

    @Test
    fun formatFromMinorUnits() {
        val calc = calculator(decimalPlaces = 2)
        assertEquals("12.50", calc.formatFromMinorUnits(1250L))
        assertEquals("0.01", calc.formatFromMinorUnits(1L))
    }

    @Test
    fun formatFromMinorUnitsZeroDecimalPlaces() {
        val calc = calculator(decimalPlaces = 0)
        assertEquals("100", calc.formatFromMinorUnits(100L))
    }

    // endregion

    // region Edge cases

    @Test
    fun rapidOperatorSwitchingKeepsLast() {
        val calc = calculator()
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressOperator(KeyboardOperator.SUBTRACT)
        calc.pressOperator(KeyboardOperator.MULTIPLY)
        val state = calc.pressOperator(KeyboardOperator.DIVIDE)
        assertTrue(state.displayExpression.contains("÷"))
    }

    @Test
    fun decimalOnlyThenDigits() {
        val calc = calculator()
        calc.pressDecimal()
        val state = calc.pressDigits(5)
        assertEquals("0.5", state.currentValue)
    }

    @Test
    fun calendarKeyReturnsCurrentState() {
        val calc = calculator()
        calc.pressDigits(5)
        val before = calc.getState()
        val after = calc.processKey(NumericKey.Calendar)
        assertEquals(before, after)
    }

    @Test
    fun currencySymbolKeyReturnsCurrentState() {
        val calc = calculator()
        calc.pressDigits(5)
        val before = calc.getState()
        val after = calc.processKey(NumericKey.CurrencySymbol)
        assertEquals(before, after)
    }

    @Test
    fun decimalWithDigitsAfterEquals() {
        val calc = calculator(decimalPlaces = 2)
        calc.pressDigits(5)
        calc.pressOperator(KeyboardOperator.ADD)
        calc.pressDigits(3)
        calc.pressEquals()
        // result is "8", decimal appends to it
        calc.pressDecimal()
        val state = calc.pressDigits(5)
        assertEquals("8.5", state.currentValue)
    }

    @Test
    fun expressionDisplayShowsCurrencyBeforeEachOperand() {
        val calc = calculator(decimalPlaces = 0)
        calc.pressDigits(2, 5)
        calc.pressOperator(KeyboardOperator.ADD)
        val state = calc.pressDigits(2, 5)
        assertEquals("25 $ + 25 $", state.displayExpression)
    }

    // endregion
}
