package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
    onNavigateToMonthlyExpenses: () -> Unit,
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
            HomeNavigation.NavigateToMonthlyExpenses -> onNavigateToMonthlyExpenses()
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
    val density = LocalDensity.current
    val collapsedBarHeightPx = with(density) { CollapsedBarHeight.toPx() }

    // Track the expanded header height after first layout
    var expandedHeightPx by remember { mutableFloatStateOf(0f) }
    var offsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (expandedHeightPx <= 0f) return Offset.Zero
                val maxOffset = -(expandedHeightPx - collapsedBarHeightPx)
                val newOffset = (offsetPx + available.y).coerceIn(maxOffset, 0f)
                val consumed = newOffset - offsetPx
                offsetPx = newOffset
                return Offset(0f, consumed)
            }
        }
    }

    val currentHeight = with(density) {
        if (expandedHeightPx > 0f) {
            (expandedHeightPx + offsetPx).coerceAtLeast(collapsedBarHeightPx).toDp()
        } else {
            // Not yet measured — show expanded
            Dp.Unspecified
        }
    }

    Scaffold(
        floatingActionButton = {
            HomeFab(onEvent = onEvent)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection),
        ) {
            CollapsingHomeHeader(
                displayName = state.displayName,
                totalSpentFormatted = state.totalSpentFormatted.orEmpty(),
                onProfileClicked = { onEvent(HomeEvent.ProfileClicked) },
                onTotalSpentClick = { onEvent(HomeEvent.TotalSpentCardClick) },
                currentHeight = currentHeight,
                modifier = if (expandedHeightPx == 0f) {
                    Modifier.onGloballyPositioned { coordinates ->
                        expandedHeightPx = coordinates.size.height.toFloat()
                    }
                } else {
                    Modifier
                },
            )

            HomeBody(
                state = state,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            )
        }
    }
}
