package com.please.stop.app.navigation.bottomnavbar

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.dog.care.navigation.routes.MainBottomTabs
import com.dog.care.utils.IconSource
import com.dog.care.utils.resolveIcon
import dogcare.composeapp.generated.resources.Res
import dogcare.composeapp.generated.resources.articles_tab
import dogcare.composeapp.generated.resources.assol_tab
import dogcare.composeapp.generated.resources.home_tab
import dogcare.composeapp.generated.resources.ic_articles_filled
import dogcare.composeapp.generated.resources.ic_articles_outlined
import dogcare.composeapp.generated.resources.ic_chat_filled
import dogcare.composeapp.generated.resources.ic_chat_outlined
import dogcare.composeapp.generated.resources.ic_home_filled
import dogcare.composeapp.generated.resources.ic_home_outlined
import dogcare.composeapp.generated.resources.ic_tasks_filled
import dogcare.composeapp.generated.resources.ic_tasks_outlined
import dogcare.composeapp.generated.resources.tasks_tab
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val bottomNavBarRoutes = persistentSetOf<MainBottomTabs>(
    MainBottomTabs.Home,
    MainBottomTabs.Calendar,
    MainBottomTabs.AiAssistant,
    MainBottomTabs.Articles
)

@Composable
fun BottomBarItemLabel(navigableRoute: MainBottomTabs) {
    val labelResId = when (navigableRoute) {
        is MainBottomTabs.Home -> TopLevelDestination.Home.label
        is MainBottomTabs.AiAssistant -> TopLevelDestination.Assol.label
        is MainBottomTabs.Calendar -> TopLevelDestination.Tasks.label
        is MainBottomTabs.Articles -> TopLevelDestination.Articles.label
    }
    Text(
        text = stringResource(labelResId),
        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.primary),
        autoSize = TextAutoSize.StepBased(maxFontSize = 12.sp)
    )
}

@Composable
fun BottomBarItemIcon(
    navigableRoute: MainBottomTabs,
    selected: Boolean
) {
    val icon = when (navigableRoute) {
        is MainBottomTabs.Home -> {
            if (selected) {
                TopLevelDestination.Home.selectedIconSource
            } else {
                TopLevelDestination.Home.unselectedIconSource
            }
        }

        is MainBottomTabs.AiAssistant -> {
            if (selected) {
                TopLevelDestination.Assol.selectedIconSource
            } else {
                TopLevelDestination.Assol.unselectedIconSource
            }
        }

        is MainBottomTabs.Calendar -> {
            if (selected) {
                TopLevelDestination.Tasks.selectedIconSource
            } else {
                TopLevelDestination.Tasks.unselectedIconSource
            }
        }

        is MainBottomTabs.Articles -> {
            if (selected) {
                TopLevelDestination.Articles.selectedIconSource
            } else {
                TopLevelDestination.Articles.unselectedIconSource
            }
        }
    }

    val labelResId = when (navigableRoute) {
        is MainBottomTabs.Home -> TopLevelDestination.Home.label
        is MainBottomTabs.AiAssistant -> TopLevelDestination.Assol.label
        is MainBottomTabs.Calendar -> TopLevelDestination.Tasks.label
        is MainBottomTabs.Articles -> TopLevelDestination.Articles.label
    }
    Icon(
        imageVector = resolveIcon(icon),
        contentDescription = stringResource(labelResId),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Immutable
internal sealed interface TopLevelDestination {

    val selectedIconSource: IconSource
    val unselectedIconSource: IconSource
    val label: StringResource

    object Home : TopLevelDestination {
        override val selectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_home_filled)
        override val unselectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_home_outlined)
        override val label: StringResource = Res.string.home_tab
    }

    object Tasks : TopLevelDestination {
        override val selectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_tasks_filled)
        override val unselectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_tasks_outlined)
        override val label: StringResource = Res.string.tasks_tab
    }

    object Assol : TopLevelDestination {
        override val selectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_chat_filled)
        override val unselectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_chat_outlined)
        override val label: StringResource = Res.string.assol_tab
    }

    object Articles : TopLevelDestination {
        override val selectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_articles_filled)
        override val unselectedIconSource: IconSource =
            IconSource.Resource(Res.drawable.ic_articles_outlined)
        override val label: StringResource = Res.string.articles_tab
    }
}
