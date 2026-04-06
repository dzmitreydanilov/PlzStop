package com.please.stop.app.utils.date

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
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
    return localDateToday(timeZone).atStartOfDayIn(timeZone).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun currentDayEndMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    val startOfTomorrow = localDateToday(timeZone)
        .plus(1, DateTimeUnit.DAY)
        .atStartOfDayIn(timeZone)
    return startOfTomorrow.minus(1, DateTimeUnit.MILLISECOND)
        .toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun now(): Instant {
    return Clock.System.now()
}

@OptIn(ExperimentalTime::class)
fun localDateToday(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Clock.System.todayIn(timeZone)

data class EpochMillisRange(val fromMillis: Long, val toMillis: Long)

fun previousMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) year - 1 to 12 else year to month - 1

fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) year + 1 to 1 else year to month + 1

fun monthName(month: Int): String =
    kotlinx.datetime.Month(month).name.lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalTime::class)
fun isBeforeCurrentMonth(
    year: Int,
    month: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    val now = localDateToday(timeZone)
    return year < now.year || (year == now.year && month < now.monthNumber)
}

@OptIn(ExperimentalTime::class)
fun isCurrentOrFutureMonth(
    year: Int,
    month: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    val now = localDateToday(timeZone)
    return year > now.year || (year == now.year && month >= now.monthNumber)
}

@OptIn(ExperimentalTime::class)
fun formatDayLabel(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date
    val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$dayOfWeek, $month ${date.dayOfMonth}"
}

@OptIn(ExperimentalTime::class)
fun monthMillisRange(
    year: Int,
    month: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): EpochMillisRange {
    val monthStart = LocalDate(year, month, 1)
    val nextMonthStart = monthStart.plus(1, DateTimeUnit.MONTH)
    return EpochMillisRange(
        fromMillis = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        toMillis = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
    )
}

@OptIn(ExperimentalTime::class)
fun currentMonthMillisRange(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): EpochMillisRange {
    val today = localDateToday(timeZone)
    val monthStart = LocalDate(today.year, today.month, 1)
    val nextMonthStart = monthStart.plus(1, DateTimeUnit.MONTH)
    return EpochMillisRange(
        fromMillis = monthStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        toMillis = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
    )
}

private const val PAGER_BASE_YEAR = 2000

fun monthPageIndex(year: Int, month: Int): Int =
    (year - PAGER_BASE_YEAR) * 12 + (month - 1)

fun pageIndexToYearMonth(pageIndex: Int): Pair<Int, Int> =
    (PAGER_BASE_YEAR + pageIndex / 12) to (pageIndex % 12 + 1)

@OptIn(ExperimentalTime::class)
fun currentMonthPageIndex(): Int =
    localDateToday().let { monthPageIndex(it.year, it.monthNumber) }

fun LocalDate.lengthOfMonth(): Int {
    val firstOfMonth = LocalDate(year, monthNumber, 1)
    val nextMonthStart = firstOfMonth.plus(1, DateTimeUnit.MONTH)
    return (nextMonthStart.toEpochDays() - firstOfMonth.toEpochDays()).toInt()
}
