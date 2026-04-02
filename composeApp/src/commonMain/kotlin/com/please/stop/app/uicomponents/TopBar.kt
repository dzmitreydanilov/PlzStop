package com.please.stop.app.uicomponents

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.please.stop.app.uicomponents.icons.ArrowBackIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoTitleTopBar(
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {},
        actions = { actions() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoTitleTopBarWithNavigation(
    navigationIcon: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {},
        navigationIcon = { navigationIcon() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NoTitleTopBarWithNavigationPreview() {
    NoTitleTopBarWithNavigation(
        navigationIcon = {
            ArrowBackIconButton({})
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun NoTitleTopBarPreview() {
    NoTitleTopBar()
}
