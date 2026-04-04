package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.home.presentation.HomeEvent
import com.please.stop.app.features.home.presentation.HomeNavigation
import com.please.stop.app.features.home.presentation.HomeState
import com.please.stop.app.features.home.presentation.HomeStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import org.koin.compose.viewmodel.koinViewModel

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
