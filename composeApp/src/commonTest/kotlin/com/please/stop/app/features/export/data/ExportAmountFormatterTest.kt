package com.please.stop.app.features.export.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExportAmountFormatterTest {

    @Test
    fun formatsPositiveMinorUnits() {
        assertEquals("123.45", formatMinorUnits(minorUnits = 12_345, decimalPlaces = 2))
    }

    @Test
    fun formatsNegativeAmountSmallerThanOneMajorUnit() {
        assertEquals("-0.05", formatMinorUnits(minorUnits = -5, decimalPlaces = 2))
    }

    @Test
    fun formatsLongMinValueWithoutOverflow() {
        assertEquals(
            "-92233720368547758.08",
            formatMinorUnits(minorUnits = Long.MIN_VALUE, decimalPlaces = 2),
        )
    }

    @Test
    fun formatsZeroDecimalCurrencies() {
        assertEquals("-123", formatMinorUnits(minorUnits = -123, decimalPlaces = 0))
    }

    @Test
    fun rejectsUnsupportedDecimalPlaces() {
        assertFailsWith<IllegalArgumentException> {
            formatMinorUnits(minorUnits = 1, decimalPlaces = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            formatMinorUnits(minorUnits = 1, decimalPlaces = 11)
        }
    }
}
