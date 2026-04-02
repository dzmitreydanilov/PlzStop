package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.please.stop.app.features.home.presentation.HomeEvent
import com.please.stop.app.features.home.presentation.HomeNavigation
import com.please.stop.app.features.home.presentation.HomeState
import com.please.stop.app.features.home.presentation.HomeStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.sheets.AppModalBottomSheet
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_add_category
import plzstop.composeapp.generated.resources.home_add_expense
import plzstop.composeapp.generated.resources.ic_add
import plzstop.composeapp.generated.resources.onboarding_add
import plzstop.composeapp.generated.resources.onboarding_cancel
import plzstop.composeapp.generated.resources.onboarding_category_name_label
import plzstop.composeapp.generated.resources.onboarding_new_category

@Composable
fun HomeScreen(
    onNavigateToAddExpense: (preselectedCategoryId: Long?) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val stateHolder = koinViewModel<HomeStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { nav ->
        when (nav) {
            is HomeNavigation.NavigateToAddExpense -> onNavigateToAddExpense(nav.preselectedCategoryId)
            HomeNavigation.NavigateToSettings -> onNavigateToSettings()
        }
    }

    when (val s = state) {
        is HomeState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is HomeState.Content -> {
            ScreenOverlayContainer(
                overlay = asOverlay(state = s),
                onDismiss = { stateHolder.processEvent(HomeEvent.DismissError) },
            ) {
                HomeContent(
                    state = s,
                    onEvent = stateHolder::processEvent,
                )
            }
        }

        is HomeState.Error -> {
            ScreenOverlayContainer(
                overlay = ScreenOverlay.Error(type = s.errorType),
                onDismiss = {},
                onRetry = {},
            ) {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun asOverlay(state: HomeState.Content): ScreenOverlay? {
    val errorType = state.errorType ?: return null
    return ScreenOverlay.Error(type = errorType)
}

@Composable
private fun HomeContent(
    state: HomeState.Content,
    onEvent: (HomeEvent) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            HomeFab(onEvent = onEvent)
        },
    ) { paddingValues ->
        HomeBody(
            state = state,
            onEvent = onEvent,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }

    if (state.showAddCategorySheet) {
        AddCategoryBottomSheet(
            onDismiss = { onEvent(HomeEvent.DismissAddCategorySheet) },
            onConfirm = { name -> onEvent(HomeEvent.ConfirmAddCategory(name)) },
        )
    }
}

@Composable
private fun HomeFab(onEvent: (HomeEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(
            onClick = { expanded = true },
            shape = CircleShape,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_add),
                contentDescription = stringResource(Res.string.home_add_expense),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.home_add_expense)) },
                onClick = {
                    expanded = false
                    onEvent(HomeEvent.AddExpenseClicked)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.home_add_category)) },
                onClick = {
                    expanded = false
                    onEvent(HomeEvent.AddCategoryClicked)
                },
            )
        }
    }
}

@Composable
private fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded,
        detents = listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded),
    )

    var name by remember { mutableStateOf("") }

    AppModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.onboarding_new_category),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text(stringResource(Res.string.onboarding_category_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.onboarding_add))
            }
        }
    }
}
