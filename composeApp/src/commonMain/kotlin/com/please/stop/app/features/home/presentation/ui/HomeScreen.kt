package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import com.please.stop.app.uicomponents.sheets.AppModalBottomSheet
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_add
import plzstop.composeapp.generated.resources.onboarding_category_name_label
import plzstop.composeapp.generated.resources.onboarding_new_category

@Composable
fun HomeScreen(
    onNavigateToAddExpense: (selectedCategoryId: Long) -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val stateHolder = koinViewModel<HomeStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            is HomeNavigation.NavigateToAddExpense -> onNavigateToAddExpense(navigation.expenseCategoryId)
            is HomeNavigation.NavigateToCreateExpense -> onNavigateToCreateExpense()
            HomeNavigation.NavigateToSettings -> onNavigateToSettings()
        }
    }
    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(HomeEvent.DismissError) },
    ) {
        DisplayFullScreenProgress(
            showProgress = state is HomeState.Loading,
        )

        HomeContent(
            state = state,
            onEvent = stateHolder::processEvent,
        )

        if (state.showAddCategorySheet) {
            AddCategoryBottomSheet(
                onDismiss = { stateHolder.processEvent(HomeEvent.DismissAddCategorySheet) },
                onConfirm = { name -> stateHolder.processEvent(HomeEvent.ConfirmAddCategory(name)) },
            )
        }
    }
}


internal val HomeState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is HomeState.Error -> ScreenOverlay.Error(type = errorType)

        else -> null
    }

@Composable
private fun HomeContent(
    state: HomeState,
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
