package com.please.stop.app.navigation.bottomnavbar

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.navigation.nav3.navigators.bottom.BottomNavigationState
import com.please.stop.app.navigation.routes.MainBottomTabs

fun bottomNavigationItems(
    hostState: BottomNavigationState,
    navigationSuiteItemColors: NavigationSuiteItemColors,
    onTabClick: (NavKey) -> Unit,
): NavigationSuiteScope.() -> Unit = {
    bottomNavBarRoutes.forEach { item ->
        val selected = item == hostState.topLevelRoute
        item(
            icon = { BottomBarItemIcon(item, selected) },
            selected = selected,
            onClick = { onTabClick(item) },
            colors = navigationSuiteItemColors,
            label = { BottomBarItemLabel(item) },
            modifier = Modifier.testTag(item.toTestTag()),
        )
    }
}

private fun MainBottomTabs.toTestTag(): String = when (this) {
    MainBottomTabs.Home -> TestTags.NAV_TAB_HOME
    MainBottomTabs.Operations -> TestTags.NAV_TAB_OPERATIONS
    MainBottomTabs.Analytics -> TestTags.NAV_TAB_ANALYTICS
    MainBottomTabs.Settings -> TestTags.NAV_TAB_SETTINGS
}
