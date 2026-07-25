package com.please.stop.app.features.export.data

import com.please.stop.app.utils.date.EpochMillisRange
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

internal fun inclusiveExportDateRange(
    startDateMillis: Long,
    endDateMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): EpochMillisRange {
    val startDate = localDateTimeFromMillis(startDateMillis, timeZone).date
    val endDate = localDateTimeFromMillis(endDateMillis, timeZone).date
    return EpochMillisRange(
        fromMillis = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        toMillis = endDate
            .plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds(),
    )
}
