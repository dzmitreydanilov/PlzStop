package com.please.stop.app.navigation.deeplink

import com.dog.care.navigation.routes.MainBottomTabs
import com.please.stop.app.navigation.routes.RemindersRoutes
import com.please.stop.app.navigation.routes.SymptomsRoute
import com.please.stop.app.navigation.routes.UserRoutes
import com.dog.care.navigation.tabs.ArticlesRoutes

/**
 * Resolves deep link URIs to navigation targets.
 *
 * Matches URL path segments against known patterns and constructs
 * the appropriate [DeepLinkResult] with a synthetic backstack.
 *
 * Supported URL patterns:
 * - `home` → Home tab
 * - `calendar` → Calendar tab
 * - `chat` → AiAssistant tab
 * - `articles` → Articles tab
 * - `article/{articleId}` → Article details (within Articles tab)
 * - `profile` → Profile screen
 * - `subscription` → Subscription details screen
 * - `reminder/create[?date=...]` → Create reminder screen
 * - `reminder/edit/{reminderId}/{recurrenceId}` → Edit reminder screen
 * - `symptoms` → Symptoms screen
 */
class DeepLinkResolver {

    /**
     * Resolves parsed deep link data into a navigation result.
     *
     * @return [DeepLinkResult] describing where to navigate, or null if the URI doesn't match
     */
    fun resolve(data: DeepLinkData): DeepLinkResult? {
        val segments = data.pathSegments
        if (segments.isEmpty()) return null

        return when (segments[0]) {
            "home" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.Home)
            "calendar" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.Calendar)
            "chat" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.AiAssistant)
            "articles" -> DeepLinkResult.TabRoute(tab = MainBottomTabs.Articles)
            "article" -> resolveArticle(segments)
            "profile" -> DeepLinkResult.GlobalRoute(
                backstack = buildSyntheticBackstack(UserRoutes.Profile)
            )
            "subscription" -> DeepLinkResult.GlobalRoute(
                backstack = buildSyntheticBackstack(UserRoutes.SubscriptionDetails)
            )
            "reminder" -> resolveReminder(segments, data.queryParams)
            "symptoms" -> DeepLinkResult.GlobalRoute(
                backstack = buildSyntheticBackstack(SymptomsRoute)
            )
            else -> null
        }
    }

    private fun resolveArticle(segments: List<String>): DeepLinkResult? {
        val articleId = segments.getOrNull(1) ?: return null
        return DeepLinkResult.TabRoute(
            tab = MainBottomTabs.Articles,
            nestedRoute = ArticlesRoutes.ArticleDetails(articleId)
        )
    }

    @Suppress("MagicNumber")
    private fun resolveReminder(
        segments: List<String>,
        queryParams: Map<String, String>
    ): DeepLinkResult? {
        val action = segments.getOrNull(1) ?: return null
        return when (action) {
            "create" -> {
                val date = queryParams["date"]
                DeepLinkResult.GlobalRoute(
                    backstack = buildSyntheticBackstack(
                        RemindersRoutes.CreateReminder(date = date, fromHome = false)
                    )
                )
            }
            "edit" -> {
                val reminderId = segments.getOrNull(2) ?: return null
                val recurrenceId = segments.getOrNull(3) ?: return null
                DeepLinkResult.GlobalRoute(
                    backstack = buildSyntheticBackstack(
                        RemindersRoutes.EditReminder(reminderId, recurrenceId)
                    )
                )
            }
            else -> null
        }
    }
}
