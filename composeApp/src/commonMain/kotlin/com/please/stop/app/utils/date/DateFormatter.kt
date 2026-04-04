package com.please.stop.app.utils.date

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringArrayResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.day_of_week
import plzstop.composeapp.generated.resources.month_names
import plzstop.composeapp.generated.resources.month_names_short

@OptIn(FormatStringsInDatetimeFormats::class)
@Composable
fun LocalDateTime.format(datePattern: DatePattern): String {
    val localDate = date
    return when (datePattern) {
        DatePattern.D_MMM_EEEE -> {
            val monthNamesRes = stringArrayResource(Res.array.month_names_short)
            val daysNamesRes = stringArrayResource(Res.array.day_of_week)
            val dayOfMonth = localDate.day
            val monthName = monthNamesRes[localDate.month.number - 1]
            val dayName = daysNamesRes[localDate.dayOfWeek.ordinal]
            "$dayOfMonth $monthName, $dayName"
        }

        DatePattern.MMMM_DD -> {
            val monthNamesRes = stringArrayResource(Res.array.month_names)
            val dayOfMonth = localDate.day
            val monthName = monthNamesRes[localDate.month.number - 1]
            "$monthName $dayOfMonth"
        }

        DatePattern.EEEE_MMM_DD -> {
            val monthNamesRes = stringArrayResource(Res.array.month_names_short)
            val daysNamesRes = stringArrayResource(Res.array.day_of_week)
            val dayOfMonth = localDate.day
            val monthName = monthNamesRes[localDate.month.number - 1]
            val dayName = daysNamesRes[localDate.dayOfWeek.ordinal]
            "$dayName, $monthName $dayOfMonth"
        }

        else -> {
            localDate.format(LocalDate.Format { byUnicodePattern(datePattern.pattern) })
        }
    }
}
