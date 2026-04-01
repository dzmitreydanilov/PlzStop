package com.dog.care.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dog.care.features.reminders.create.presentation.CreateReminderRoute
import com.dog.care.features.reminders.edit.presentation.EditReminderRoute
import com.dog.care.features.reminders.root.presentation.CalendarScreenRoute
import com.dog.care.features.reminders.search.presentation.SearchRemindersRoute
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.routes.AuthRoute
import com.dog.care.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.RemindersRoutes

internal fun EntryProviderScope<NavKey>.calendarTabEntries(
    onNavigateSignIn: (AuthRoute) -> Unit,
    onNavigateReminder: (RemindersRoutes) -> Unit,
) {
    entry<MainBottomTabs.Calendar> {
        CalendarScreenRoute(
            onNavigateSearch = { onNavigateReminder(RemindersRoutes.SearchReminders) },
            onCreateReminderClick = {
                onNavigateReminder(
                    RemindersRoutes.CreateReminder(
                        date = it,
                        fromHome = false
                    )
                )
            },
            onEditReminderClick = { id, date ->
                onNavigateReminder(RemindersRoutes.EditReminder(id, date))
            },
            onSelectAuthMethod = { onNavigateSignIn(AuthRoute.ChooseSignInOption) }
        )
    }
}

internal fun EntryProviderScope<NavKey>.remindersEntries(
    router: Router<NavKey>,
    onBack: () -> Unit
) {
    entry<RemindersRoutes.SearchReminders> {
        SearchRemindersRoute(
            onNavigateEditReminder = { id, date ->
                router.replaceCurrent(
                    RemindersRoutes.EditReminder(id, date)
                )
            }
        )
    }
    entry<RemindersRoutes.CreateReminder> { (selectedDate, provideNavigationResult) ->
        CreateReminderRoute(
            onNavigateBack = onBack,
            date = selectedDate,
            setNavigationResult = provideNavigationResult
        )
    }
    entry<RemindersRoutes.EditReminder> { (reminderId, recurrenceId) ->
        EditReminderRoute(
            reminderId = reminderId,
            recurrenceId = recurrenceId,
            onNavigateBack = onBack
        )
    }
}
