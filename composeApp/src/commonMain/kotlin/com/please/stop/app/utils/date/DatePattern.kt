package com.please.stop.app.utils.date

import androidx.compose.runtime.Stable

@Stable
enum class DatePattern(val pattern: String) {
    D_MMMM_YYYY("d MMMM yyyy"),
    YYYY_MM_DD("yyyy-MM-dd"),
    MMMM_DD("MMMM dd"),
    DD_MMMM("dd MMMM"),
    D_MMM_EEEE("d MMM, EEEE"),
    EEEE_MMM_DD("EEEE, MMM dd")
}
