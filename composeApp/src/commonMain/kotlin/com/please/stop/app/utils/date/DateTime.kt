package com.please.stop.app.utils.date

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun localDateTimeSince(
    fromEpochSeconds: Long = 0,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    return Instant.fromEpochSeconds(fromEpochSeconds)
        .toLocalDateTime(timeZone)
}

@OptIn(ExperimentalTime::class)
fun localDateTimeNowAsString(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    return now().toLocalDateTime(timeZone).toString()
}

@OptIn(ExperimentalTime::class)
fun localDateTimeNow(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    return now().toLocalDateTime(timeZone)
}

@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = now().toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
fun localDateTimeFromMillis(
    timeStamp: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    return Instant.fromEpochMilliseconds(timeStamp)
        .toLocalDateTime(timeZone)
}

@OptIn(ExperimentalTime::class)
fun currentDayStartMillis(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long {
    return Clock.System.todayIn(timeZone).atStartOfDayIn(timeZone).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun currentDayEndMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    val startOfTomorrow = Clock.System.todayIn(timeZone)
        .plus(1, DateTimeUnit.DAY)
        .atStartOfDayIn(timeZone)
    return startOfTomorrow.minus(1, DateTimeUnit.MILLISECOND)
        .toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun now(): Instant {
    return Clock.System.now()
}
