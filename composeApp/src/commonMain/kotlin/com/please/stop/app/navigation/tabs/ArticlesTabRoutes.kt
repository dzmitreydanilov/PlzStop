package com.dog.care.navigation.tabs

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dog.care.features.articles.article.presentation.ArticleScreenRoute
import com.dog.care.features.articles.list.ArticlesScreenRoute
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.routes.AuthRoute
import com.dog.care.navigation.routes.MainBottomTabs
import kotlinx.serialization.Serializable


internal fun EntryProviderScope<NavKey>.articlesTabEntries(
    router: Router<NavKey>,
    onNavigateSignIn: (AuthRoute) -> Unit
) {
    entry<MainBottomTabs.Articles> {
        ArticlesScreenRoute(
            onArticleClick = { id ->
                router.push(ArticlesRoutes.ArticleDetails(id))
            },
            onNavigateSignIn = {
                onNavigateSignIn(AuthRoute.ChooseSignInOption)
            }
        )
    }

    entry<ArticlesRoutes.ArticleDetails> { route ->
        ArticleScreenRoute(
            id = route.articleId,
            onNavigateBack = router::pop
        )
    }
}


sealed interface ArticlesRoutes : NavKey {

    @Serializable
    data class ArticleDetails(val articleId: String) : ArticlesRoutes
}
