package com.please.stop.app.features.export.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportDateRangeTest {

    private val timeZone = TimeZone.of("Europe/Warsaw")

    @Test
    fun singleDayIncludesTheWholeSelectedDay() {
        val selectedDay = LocalDate(2026, 7, 16).atStartOfDayIn(timeZone).toEpochMilliseconds()

        val result = inclusiveExportDateRange(
            startDateMillis = selectedDay,
            endDateMillis = selectedDay,
            timeZone = timeZone,
        )

        assertEquals(selectedDay, result.fromMillis)
        assertEquals(
            LocalDate(2026, 7, 17).atStartOfDayIn(timeZone).toEpochMilliseconds(),
            result.toMillis,
        )
    }

    @Test
    fun rangeIncludesTheWholeFinalDay() {
        val selectedStart = LocalDate(2026, 7, 14).atStartOfDayIn(timeZone).toEpochMilliseconds()
        val selectedEnd = LocalDate(2026, 7, 16).atStartOfDayIn(timeZone).toEpochMilliseconds()

        val result = inclusiveExportDateRange(
            startDateMillis = selectedStart,
            endDateMillis = selectedEnd,
            timeZone = timeZone,
        )

        assertEquals(selectedStart, result.fromMillis)
        assertEquals(
            LocalDate(2026, 7, 17).atStartOfDayIn(timeZone).toEpochMilliseconds(),
            result.toMillis,
        )
    }
}
