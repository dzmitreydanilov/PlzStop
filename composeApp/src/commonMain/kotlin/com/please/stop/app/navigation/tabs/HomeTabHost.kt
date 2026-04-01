package com.please.stop.app.navigation.tabs

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.routes.AuthRoute
import com.dog.care.navigation.routes.MainBottomTabs
import com.dog.care.navigation.tabs.ArticlesRoutes
import com.please.stop.app.navigation.routes.RemindersRoutes

@Composable
fun HomeTabHost(
    onNavigateSignIn: () -> Unit,
    onNavigatePreSignIn: () -> Unit,
    onNavigateSymptom: () -> Unit,
    onReminderClick: (String, String) -> Unit,
    onCreateReminder: () -> Unit,
    onSearchReminderClick: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenArticleDetails: (String) -> Unit = {},
) {
    HomeScreenRoute(
        onNavigateProfile = onOpenProfile,
        onNavigateRemindersSearch = onSearchReminderClick,
        onNavigateArticlesFromStories = onOpenArticleDetails,
        onNavigatePreSignIn = onNavigatePreSignIn,
        onNavigateLogin = onNavigateSignIn,
        onAddSymptomsClick = onNavigateSymptom,
        onReminderClick = onReminderClick,
        onCreateReminder = onCreateReminder
    )
}

internal fun EntryProviderScope<NavKey>.homeTabEntries(
    onNavigateProfile: () -> Unit,
    router: Router<NavKey>,
    onNavigateSignIn: (AuthRoute) -> Unit,
    onNavigateSymptoms: () -> Unit,
    onNavigateReminder: (RemindersRoutes) -> Unit
) {
    entry<MainBottomTabs.Home> {
        HomeTabHost(
            onOpenProfile = onNavigateProfile,
            onOpenArticleDetails = { articleId ->
                router.replaceCurrent(MainBottomTabs.Articles)
                router.push(ArticlesRoutes.ArticleDetails(articleId))
            },
            onNavigateSignIn = { onNavigateSignIn(AuthRoute.ChooseSignInOption) },
            onNavigatePreSignIn = { onNavigateSignIn(AuthRoute.PreSignIn) },
            onNavigateSymptom = onNavigateSymptoms,
            onReminderClick = { id, date ->
                onNavigateReminder(
                    RemindersRoutes.EditReminder(id, date)
                )
            },
            onCreateReminder = {
                onNavigateReminder(
                    RemindersRoutes.CreateReminder(
                        date = null,
                        fromHome = true
                    )
                )
            },
            onSearchReminderClick = { onNavigateReminder(RemindersRoutes.SearchReminders) }
        )
    }
}
