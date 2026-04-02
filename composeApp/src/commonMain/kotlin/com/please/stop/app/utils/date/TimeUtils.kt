package com.please.stop.app.utils.date

import kotlinx.datetime.LocalTime

private const val TWELVE_HOUR_CLOCK_BASE = 12
fun formatLocalTimeToHMMAMPM(time: LocalTime?, amPMLowerCase: Boolean = false): String {
    val currentTime = time ?: localDateTimeNow().time
    val hour12 =
        if (currentTime.hour % TWELVE_HOUR_CLOCK_BASE == 0) {
            TWELVE_HOUR_CLOCK_BASE
        } else {
            currentTime.hour % TWELVE_HOUR_CLOCK_BASE
        }
    val minute = currentTime.minute.toString().padStart(2, '0')
    val amPm = if (currentTime.hour < TWELVE_HOUR_CLOCK_BASE) "AM" else "PM"
    return "$hour12:$minute ${amPm.toLowerCase(amPMLowerCase)}"
}

private fun String.toLowerCase(toLowerCase: Boolean): String {
    return if (toLowerCase) {
        this.lowercase()
    } else {
        this
    }
}
