package com.please.stop.app.features.analytics.presentation

sealed interface AnalyticsEvent {
    data class DayTapped(val dayOfMonth: Int) : AnalyticsEvent
    data object DismissDaySheet : AnalyticsEvent
    data object DismissError : AnalyticsEvent
}
