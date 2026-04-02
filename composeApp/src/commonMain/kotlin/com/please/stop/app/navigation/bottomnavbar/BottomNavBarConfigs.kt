package com.please.stop.app.navigation.bottomnavbar

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.please.stop.app.navigation.routes.MainBottomTabs
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_tab
import plzstop.composeapp.generated.resources.home_tab
import plzstop.composeapp.generated.resources.ic_analytics_filled
import plzstop.composeapp.generated.resources.ic_analytics_outlined
import plzstop.composeapp.generated.resources.ic_home_filled
import plzstop.composeapp.generated.resources.ic_home_outlined
import plzstop.composeapp.generated.resources.ic_settings
import plzstop.composeapp.generated.resources.ic_settings_filled
import plzstop.composeapp.generated.resources.settings_tab

val bottomNavBarRoutes = persistentSetOf<MainBottomTabs>(
    MainBottomTabs.Home,
    MainBottomTabs.Analytics,
    MainBottomTabs.Settings,
)

@Composable
fun BottomBarItemLabel(navigableRoute: MainBottomTabs) {
    val labelResId = navigableRoute.toDestination().label
    Text(
        text = stringResource(labelResId),
        style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.primary),
    )
}

@Composable
fun BottomBarItemIcon(
    navigableRoute: MainBottomTabs,
    selected: Boolean,
) {
    val destination = navigableRoute.toDestination()
    val icon = if (selected) destination.selectedIcon else destination.unselectedIcon
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(destination.label),
        tint = MaterialTheme.colorScheme.primary,
    )
}

private fun MainBottomTabs.toDestination(): TopLevelDestination = when (this) {
    MainBottomTabs.Home -> TopLevelDestination.Home
    MainBottomTabs.Analytics -> TopLevelDestination.Analytics
    MainBottomTabs.Settings -> TopLevelDestination.Settings
}

@Immutable
internal sealed interface TopLevelDestination {

    val selectedIcon: DrawableResource
    val unselectedIcon: DrawableResource
    val label: StringResource

    data object Home : TopLevelDestination {
        override val selectedIcon = Res.drawable.ic_home_filled
        override val unselectedIcon = Res.drawable.ic_home_outlined
        override val label = Res.string.home_tab
    }

    data object Analytics : TopLevelDestination {
        override val selectedIcon = Res.drawable.ic_analytics_filled
        override val unselectedIcon = Res.drawable.ic_analytics_outlined
        override val label = Res.string.analytics_tab
    }

    data object Settings : TopLevelDestination {
        override val selectedIcon = Res.drawable.ic_settings_filled
        override val unselectedIcon = Res.drawable.ic_settings
        override val label = Res.string.settings_tab
    }
}
